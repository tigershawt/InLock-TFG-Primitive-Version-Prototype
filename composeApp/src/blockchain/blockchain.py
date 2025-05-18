import json
import hashlib
import time
import uuid
from typing import Dict, List, Optional, Tuple, Set, Any
import os
import random
import logging
import argparse
from flask import Flask, request, jsonify

logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(name)s - %(levelname)s - %(message)s')
logger = logging.getLogger('inlock_api')

class Node:
    VALID_ACTIONS = {"register", "transfer"}

    def __init__(
        self,
        asset_id: str,
        action: str,
        user_id: str,
        timestamp: float = None,
        references: List[str] = None,
        signature: str = None,
        node_id: str = None,
        data: Dict[str, Any] = None
    ):
        if not asset_id:
            raise ValueError("Asset ID cannot be empty")
        if action not in self.VALID_ACTIONS:
            raise ValueError(f"Invalid action: {action}. Must be one of {self.VALID_ACTIONS}")
        if not user_id:
            raise ValueError("User ID cannot be empty")

        self.asset_id = asset_id
        self.action = action
        self.user_id = user_id
        self.timestamp = timestamp if timestamp is not None else time.time()
        self.references = references if references is not None else []
        self.data = data if data is not None else {}
        self.signature = signature if signature is not None else self._generate_signature()
        self.node_id = node_id if node_id is not None else str(uuid.uuid4())
        self.hash = self._calculate_hash()

    def _generate_signature(self) -> str:
        signature_base = f"{self.user_id}:{self.timestamp}:{random.randint(1, 1000000)}"
        return hashlib.sha256(signature_base.encode()).hexdigest()

    def _calculate_hash(self) -> str:
        content = (
            f"{self.asset_id}:{self.action}:{self.user_id}:"
            f"{self.timestamp}:{':'.join(self.references)}:"
            f"{self.signature}:{json.dumps(self.data, sort_keys=True)}"
        )
        return hashlib.sha256(content.encode()).hexdigest()

    def to_dict(self) -> Dict:
        return {
            "node_id": self.node_id,
            "asset_id": self.asset_id,
            "action": self.action,
            "user_id": self.user_id,
            "timestamp": self.timestamp,
            "references": self.references,
            "signature": self.signature,
            "hash": self.hash,
            "data": self.data
        }

    @classmethod
    def from_dict(cls, data: Dict) -> 'Node':
        return cls(
            asset_id=data["asset_id"],
            action=data["action"],
            user_id=data["user_id"],
            timestamp=data["timestamp"],
            references=data["references"],
            signature=data["signature"],
            node_id=data["node_id"],
            data=data.get("data", {})
        )

class DAG:
    def __init__(self, storage_path: str = "blockchain_dag.json"):
        self.nodes: Dict[str, Node] = {}
        self.tips: Set[str] = set()
        self.storage_path = storage_path

        self._write_lock = False

        if os.path.exists(storage_path):
            try:
                self.load()
                logger.info(f"Blockchain loaded from {storage_path} with {len(self.nodes)} nodes")
            except Exception as e:
                logger.error(f"Failed to load blockchain from {storage_path}: {str(e)}", exc_info=True)
                self.nodes = {}
                self.tips = set()

    def add_node(self, node: Node) -> Tuple[bool, str]:
        try:
            valid, message = self._validate_node(node)
            if not valid:
                logger.warning(f"Node validation failed: {message}")
                return False, message

            self.nodes[node.node_id] = node

            for ref in node.references:
                if ref in self.tips:
                    self.tips.remove(ref)
            self.tips.add(node.node_id)

            logger.info(f"Added node: ID={node.node_id}, Action={node.action}, Asset={node.asset_id}, User={node.user_id}")

            self.save()

            return True, node.node_id

        except Exception as e:
            error_msg = f"Error adding node: {str(e)}"
            logger.error(error_msg, exc_info=True)
            return False, error_msg

    def _validate_node(self, node: Node) -> Tuple[bool, str]:
        if node.node_id in self.nodes:
            return False, f"Node with ID {node.node_id} already exists"

        for ref in node.references:
            if ref not in self.nodes:
                return False, f"Referenced node {ref} does not exist"

        if len(node.references) > 2:
            return False, "A node cannot have more than 2 references"

        if node.action == "register":
            for existing_node in self.nodes.values():
                if existing_node.asset_id == node.asset_id and existing_node.action == "register":
                    return False, f"Asset {node.asset_id} is already registered"

            if not node.data:
                logger.warning(f"Asset {node.asset_id} registered without metadata")

        elif node.action == "transfer":
            owner_history = self.get_asset_ownership_history(node.asset_id)

            if not owner_history:
                return False, f"Asset {node.asset_id} is not registered"

            current_owner = owner_history[-1]["user_id"]

            if current_owner != node.user_id:
                return False, f"Transfer requested by {node.user_id}, but asset is owned by {current_owner}"

            if "recipient_id" not in node.data:
                return False, "Transfer must include a recipient_id in the data"

            if node.data["recipient_id"] == node.user_id:
                return False, "Cannot transfer asset to yourself"

            if not node.data["recipient_id"]:
                return False, "Recipient ID cannot be empty"


        return True, "Node is valid"

    def get_node(self, node_id: str) -> Optional[Node]:
        return self.nodes.get(node_id)

    def get_asset_nodes(self, asset_id: str) -> List[Node]:
        return [node for node in self.nodes.values() if node.asset_id == asset_id]

    def get_user_nodes(self, user_id: str) -> List[Node]:
        return [node for node in self.nodes.values() if node.user_id == user_id]

    def get_asset_ownership_history(self, asset_id: str) -> List[Dict]:
        asset_nodes = self.get_asset_nodes(asset_id)
        if not asset_nodes:
            return []

        asset_nodes.sort(key=lambda x: x.timestamp)

        ownership_history = []

        for node in asset_nodes:
            if node.action == "register":
                ownership_history.append({
                    "user_id": node.user_id,
                    "timestamp": node.timestamp,
                    "node_id": node.node_id,
                    "action": "register"
                })

            elif node.action == "transfer" and "recipient_id" in node.data:
                ownership_history.append({
                    "user_id": node.data["recipient_id"],
                    "timestamp": node.timestamp,
                    "node_id": node.node_id,
                    "action": "transfer"
                })

        return ownership_history

    def get_asset_staking_status(self, asset_id: str) -> Dict:
        ownership_history = self.get_asset_ownership_history(asset_id)
        if not ownership_history:
            return {"is_staked": False, "error": "Asset not found"}
            
        current_owner = ownership_history[-1]["user_id"]
        
        return {
            "is_staked": False,
            "owner_id": current_owner
        }

    def get_user_assets(self, user_id: str) -> List[str]:
        all_assets = set()
        owned_assets = set()

        for node in self.nodes.values():
            all_assets.add(node.asset_id)

        for asset_id in all_assets:
            ownership_history = self.get_asset_ownership_history(asset_id)
            if ownership_history and ownership_history[-1]["user_id"] == user_id:
                owned_assets.add(asset_id)

        return list(owned_assets)

    def get_user_staking_balance(self, user_id: str) -> int:
        return 0

    def save(self):
        if self._write_lock:
            logger.warning("Blockchain save attempted while another save was in progress")
            return

        try:
            self._write_lock = True

            if os.path.exists(self.storage_path):
                backup_path = f"{self.storage_path}.bak"
                try:
                    import shutil
                    shutil.copy2(self.storage_path, backup_path)
                except Exception as e:
                    logger.error(f"Failed to create backup: {str(e)}")

            data = {
                "nodes": {node_id: node.to_dict() for node_id, node in self.nodes.items()},
                "tips": list(self.tips)
            }

            temp_path = f"{self.storage_path}.tmp"
            with open(temp_path, "w") as f:
                json.dump(data, f, indent=2)

            os.replace(temp_path, self.storage_path)

            logger.info(f"Blockchain saved with {len(self.nodes)} nodes")

        except Exception as e:
            logger.error(f"Error saving blockchain: {str(e)}", exc_info=True)
        finally:
            self._write_lock = False

    def load(self):
        try:
            with open(self.storage_path, "r") as f:
                data = json.load(f)

            self.nodes = {
                node_id: Node.from_dict(node_data)
                for node_id, node_data in data["nodes"].items()
            }

            self.tips = set(data["tips"])

            logger.info(f"Loaded {len(self.nodes)} nodes from blockchain storage")

        except json.JSONDecodeError as e:
            logger.error(f"JSON error loading blockchain: {str(e)}", exc_info=True)

            backup_path = f"{self.storage_path}.bak"
            if os.path.exists(backup_path):
                logger.info(f"Attempting to restore from backup {backup_path}")
                with open(backup_path, "r") as f:
                    data = json.load(f)

                self.nodes = {
                    node_id: Node.from_dict(node_data)
                    for node_id, node_data in data["nodes"].items()
                }
                self.tips = set(data["tips"])
            else:
                raise

        except Exception as e:
            logger.error(f"Error loading blockchain: {str(e)}", exc_info=True)
            raise

    def get_tips(self) -> List[Node]:
        return [self.nodes[node_id] for node_id in self.tips]

    def choose_references(self) -> List[str]:
        tips_list = list(self.tips)

        if len(tips_list) >= 2:
            return random.sample(tips_list, 2)

        return tips_list

    def verify_integrity(self) -> Tuple[bool, str]:
        try:
            logger.info("Verifying blockchain reference integrity...")
            for node_id, node in self.nodes.items():
                for ref in node.references:
                    if ref not in self.nodes:
                        return False, f"Node {node_id} references non-existent node {ref}"

            logger.info("Verifying blockchain hash integrity...")
            for node_id, node in self.nodes.items():
                expected_hash = node._calculate_hash()
                if node.hash != expected_hash:
                    return False, f"Hash mismatch for node {node_id}"

            logger.info("Verifying asset ownership chains...")
            asset_ids = set(node.asset_id for node in self.nodes.values())
            for asset_id in asset_ids:
                ownership_history = self.get_asset_ownership_history(asset_id)
                if not ownership_history:
                    continue

                for i in range(1, len(ownership_history)):
                    prev = ownership_history[i-1]
                    curr = ownership_history[i]

                    if curr["action"] == "transfer":
                        transfer_node = self.nodes.get(curr["node_id"])
                        if transfer_node is None:
                            return False, f"Missing transfer node {curr['node_id']}"

                        if transfer_node.user_id != prev["user_id"]:
                            return False, f"Transfer node {curr['node_id']} has invalid initiator"

            logger.info("Verifying DAG tips...")
            computed_tips = set()
            referenced_nodes = set()

            for node in self.nodes.values():
                referenced_nodes.update(node.references)

            for node_id in self.nodes:
                if node_id not in referenced_nodes:
                    computed_tips.add(node_id)

            if computed_tips != self.tips:
                extra_tips = self.tips - computed_tips
                missing_tips = computed_tips - self.tips

                error_msg = "Tips inconsistency detected. "
                if extra_tips:
                    error_msg += f"Extra tips: {extra_tips}. "
                if missing_tips:
                    error_msg += f"Missing tips: {missing_tips}."

                logger.warning(f"{error_msg} Auto-fixing...")
                self.tips = computed_tips
                self.save()

            logger.info("Blockchain integrity verified successfully")
            return True, "DAG integrity verified"

        except Exception as e:
            error_msg = f"Integrity verification error: {str(e)}"
            logger.error(error_msg, exc_info=True)
            return False, error_msg

def register_asset(blockchain: DAG, asset_id: str, user_id: str, asset_data: Dict = None) -> Tuple[bool, str]:
    references = blockchain.choose_references()

    node = Node(
        asset_id=asset_id,
        action="register",
        user_id=user_id,
        references=references,
        data=asset_data or {}
    )

    return blockchain.add_node(node)

def transfer_asset(blockchain: DAG, asset_id: str, from_user_id: str, to_user_id: str) -> Tuple[bool, str]:
    if not verify_asset_ownership(blockchain, asset_id, from_user_id):
        return False, f"Asset {asset_id} is not owned by {from_user_id}"

    if verify_asset_ownership(blockchain, asset_id, to_user_id):
        return False, f"Asset {asset_id} is already owned by {to_user_id}"

    references = blockchain.choose_references()

    node = Node(
        asset_id=asset_id,
        action="transfer",
        user_id=from_user_id,
        references=references,
        data={
            "recipient_id": to_user_id,
            "transfer_timestamp": time.time(),
            "status": "completed"
        }
    )

    return blockchain.add_node(node)

def stake_asset(blockchain: DAG, asset_id: str, user_id: str, staking_amount: int = 2400) -> Tuple[bool, str]:
    return False, "Staking functionality has been removed"

def verify_asset_ownership(blockchain: DAG, asset_id: str, user_id: str) -> bool:
    try:
        ownership_history = blockchain.get_asset_ownership_history(asset_id)

        if not ownership_history:
            logger.warning(f"Ownership verification failed: Asset {asset_id} not found")
            return False

        is_owner = ownership_history[-1]["user_id"] == user_id

        if is_owner:
            logger.info(f"Ownership verified: Asset {asset_id} is owned by {user_id}")
        else:
            current_owner = ownership_history[-1]["user_id"]
            logger.warning(f"Ownership verification failed: Asset {asset_id} is owned by {current_owner}, not {user_id}")

        return is_owner

    except Exception as e:
        logger.error(f"Error verifying ownership: {str(e)}", exc_info=True)
        return False

app = Flask(__name__)
blockchain = DAG("blockchain_dag.json")

@app.route('/health', methods=['GET'])
def health_check():
    return jsonify({"status": "ok", "service": "InLock Blockchain API"})

@app.route('/process_nfc_tag', methods=['POST'])
def process_nfc_tag():
    try:
        data = request.json
        tag_id = data.get('tag_id')
        user_id = data.get('user_id')

        if not tag_id or not user_id:
            return jsonify({"success": False, "message": "Missing tag_id or user_id"}), 400

        asset_data = {
            "tag_type": data.get('tag_type', 'NFC'),
            "tag_technologies": data.get('tag_technologies', []),
            "ndef_message": data.get('ndef_message', ''),
            "scanned_timestamp": data.get('timestamp', 0)
        }

        asset_exists = False
        for node in blockchain.nodes.values():
            if node.asset_id == tag_id and node.action == "register":
                asset_exists = True
                break

        if asset_exists:
            return jsonify({
                "success": False,
                "message": "Asset already exists. Staking functionality has been removed.",
                "action": "none",
                "asset_id": tag_id
            })
        else:
            success, result = register_asset(blockchain, tag_id, user_id, asset_data)
            return jsonify({
                "success": success,
                "result": result,
                "action": "register",
                "asset_id": tag_id
            })

    except Exception as e:
        logger.error(f"Error processing NFC tag: {str(e)}", exc_info=True)
        return jsonify({"success": False, "message": f"Server error: {str(e)}"}), 500

@app.route('/register_asset', methods=['POST'])
def api_register_asset():
    try:
        data = request.json
        asset_id = data.get('asset_id')
        user_id = data.get('user_id')
        asset_data = data.get('asset_data', {})

        if not asset_id or not user_id:
            return jsonify({"success": False, "message": "Missing required fields"}), 400

        success, result = register_asset(blockchain, asset_id, user_id, asset_data)
        return jsonify({"success": success, "result": result})
    except Exception as e:
        logger.error(f"Error in register_asset: {str(e)}", exc_info=True)
        return jsonify({"success": False, "message": f"Server error: {str(e)}"}), 500

@app.route('/transfer_asset', methods=['POST'])
def api_transfer_asset():
    try:
        data = request.json
        asset_id = data.get('asset_id')
        from_user_id = data.get('from_user_id')
        to_user_id = data.get('to_user_id')

        if not asset_id or not from_user_id or not to_user_id:
            return jsonify({"success": False, "message": "Missing required fields"}), 400

        logger.info(f"Transfer asset request: {asset_id} from {from_user_id} to {to_user_id}")

        if not verify_asset_ownership(blockchain, asset_id, from_user_id):
            return jsonify({
                "success": False,
                "result": f"Asset {asset_id} is not owned by {from_user_id}"
            })

        success, result = transfer_asset(blockchain, asset_id, from_user_id, to_user_id)

        logger.info(f"Transfer result: success={success}, result={result}")

        if success:
            return jsonify({"success": True, "result": result})
        else:
            return jsonify({"success": False, "result": result})

    except Exception as e:
        logger.error(f"Error in transfer_asset: {str(e)}", exc_info=True)
        return jsonify({"success": False, "message": f"Server error: {str(e)}"}), 500

@app.route('/stake_asset', methods=['POST'])
def api_stake_asset():
    return jsonify({"success": False, "message": "Staking functionality has been removed"}), 400

@app.route('/asset_staking_status/<asset_id>', methods=['GET'])
def api_asset_staking_status(asset_id):
    return jsonify({"success": False, "message": "Staking functionality has been removed"}), 400

@app.route('/user_balance/<user_id>', methods=['GET'])
def api_user_balance(user_id):
    return jsonify({"user_id": user_id, "balance": 0})

@app.route('/user_assets/<user_id>', methods=['GET'])
def api_user_assets(user_id):
    try:
        assets = blockchain.get_user_assets(user_id)
        return jsonify({"user_id": user_id, "assets": assets})
    except Exception as e:
        logger.error(f"Error in user_assets: {str(e)}", exc_info=True)
        return jsonify({"success": False, "message": f"Server error: {str(e)}"}), 500

@app.route('/verify_ownership', methods=['GET'])
def api_verify_ownership():
    try:
        asset_id = request.args.get('asset_id')
        user_id = request.args.get('user_id')

        if not asset_id or not user_id:
            logger.warning(f"Missing parameters in verify_ownership: asset_id={asset_id}, user_id={user_id}")
            return jsonify({"success": False, "message": "Missing required parameters"}), 400

        logger.info(f"Verifying ownership of asset {asset_id} for user {user_id}")

        is_owner = verify_asset_ownership(blockchain, asset_id, user_id)

        if is_owner:
            logger.info(f"Verification successful: {user_id} owns {asset_id}")
            return jsonify({
                "success": True,
                "asset_id": asset_id,
                "user_id": user_id,
                "is_owner": True
            })
        else:
            ownership_history = blockchain.get_asset_ownership_history(asset_id)
            current_owner = ownership_history[-1]["user_id"] if ownership_history else "unknown"

            logger.info(f"Verification failed: {user_id} does not own {asset_id}, current owner is {current_owner}")
            return jsonify({
                "success": True,
                "asset_id": asset_id,
                "user_id": user_id,
                "is_owner": False,
                "current_owner": current_owner
            })
    except Exception as e:
        logger.error(f"Error in verify_ownership: {str(e)}", exc_info=True)
        return jsonify({"success": False, "message": f"Server error: {str(e)}"}), 500

@app.route('/asset_history/<asset_id>', methods=['GET'])
def api_asset_history(asset_id):
    try:
        history = blockchain.get_asset_ownership_history(asset_id)
        return jsonify({"asset_id": asset_id, "history": history})
    except Exception as e:
        logger.error(f"Error in asset_history: {str(e)}", exc_info=True)
        return jsonify({"success": False, "message": f"Server error: {str(e)}"}), 500

@app.route('/asset_data/<asset_id>', methods=['GET'])
def api_asset_data(asset_id):
    try:
        asset_nodes = blockchain.get_asset_nodes(asset_id)
        register_node = next((node for node in asset_nodes if node.action == "register"), None)

        data = {}
        if register_node and register_node.data:
            data = {k: str(v) for k, v in register_node.data.items()}

        return jsonify({"asset_id": asset_id, "data": data})
    except Exception as e:
        logger.error(f"Error in asset_data: {str(e)}", exc_info=True)
        return jsonify({"success": False, "message": f"Server error: {str(e)}"}), 500

@app.route('/verify_integrity', methods=['GET'])
def api_verify_integrity():
    try:
        integrity_ok, message = blockchain.verify_integrity()
        return jsonify({"integrity_ok": integrity_ok, "message": message})
    except Exception as e:
        logger.error(f"Error in verify_integrity: {str(e)}", exc_info=True)
        return jsonify({"success": False, "message": f"Server error: {str(e)}"}), 500

@app.route('/blockchain_stats', methods=['GET'])
def api_blockchain_stats():
    try:
        stats = {
            "total_nodes": len(blockchain.nodes),
            "total_tips": len(blockchain.tips),
            "unique_assets": len(set(node.asset_id for node in blockchain.nodes.values())),
            "unique_users": len(set(node.user_id for node in blockchain.nodes.values())),
            "action_counts": {
                "register": len([node for node in blockchain.nodes.values() if node.action == "register"]),
                "transfer": len([node for node in blockchain.nodes.values() if node.action == "transfer"])
            }
        }
        return jsonify({"success": True, "stats": stats})
    except Exception as e:
        logger.error(f"Error in blockchain_stats: {str(e)}", exc_info=True)
        return jsonify({"success": False, "message": f"Server error: {str(e)}"}), 500

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='Start a blockchain node')
    parser.add_argument('--port', type=int, default=5001, help='Port to run the blockchain on')
    parser.add_argument('--storage', type=str, default="blockchain_dag.json", help='Path to storage file')
    args = parser.parse_args()

    blockchain = DAG(args.storage)

    logger.info(f"Starting blockchain node on port {args.port} with storage {args.storage}")
    app.run(host='0.0.0.0', port=args.port)
