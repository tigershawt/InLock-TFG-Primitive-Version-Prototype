import json
import requests
import random
import time
import threading
import logging
import os
from flask import Flask, request, jsonify
from typing import List, Dict, Any, Tuple, Set, Optional
from concurrent.futures import ThreadPoolExecutor, as_completed

logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(name)s - %(levelname)s - %(message)s')
logger = logging.getLogger('blockchain_orchestrator')

class BlockchainOrchestrator:

    def __init__(self, blockchain_ports: List[int] = None):
        self.blockchain_ports = blockchain_ports or [5001, 5002, 5003, 5004, 5005, 5006, 5007]
        self.base_urls = [f"http://localhost:{port}" for port in self.blockchain_ports]
        self.min_consensus = 3
        self.executor = ThreadPoolExecutor(max_workers=len(self.blockchain_ports))
        logger.info(f"Initialized orchestrator with {len(self.blockchain_ports)} blockchain instances")
        logger.info(f"Blockchain URLs: {self.base_urls}")
        self.active_urls = self._check_active_blockchains()

        if len(self.active_urls) < self.min_consensus:
            logger.warning(f"Not enough active blockchains ({len(self.active_urls)}/{self.min_consensus} required)")
        else:
            logger.info(f"Found {len(self.active_urls)} active blockchain instances")

    def _check_active_blockchains(self) -> List[str]:

        active_urls = []

        def check_health(url):
            try:
                response = requests.get(f"{url}/health", timeout=2)
                if response.status_code == 200:
                    return url
            except Exception as e:
                logger.debug(f"Blockchain at {url} not responding: {str(e)}")
            return None

        futures = [self.executor.submit(check_health, url) for url in self.base_urls]
        for future in as_completed(futures):
            result = future.result()
            if result:
                active_urls.append(result)

        return active_urls

    def register_asset(self, asset_id: str, user_id: str, asset_data: Dict[str, Any]) -> Tuple[bool, str, List[str]]:

        if len(self.active_urls) < self.min_consensus:
            return False, f"Not enough active blockchain instances ({len(self.active_urls)}/{self.min_consensus})", []

        self.active_urls = self._check_active_blockchains()

        target_count = min(len(self.active_urls), max(self.min_consensus, 3))
        selected_urls = random.sample(self.active_urls, target_count)

        logger.info(f"Registering asset {asset_id} for user {user_id} across {target_count} blockchain instances")

        registration_data = {
            "asset_id": asset_id,
            "user_id": user_id,
            "asset_data": asset_data
        }

        successes = []
        node_ids = []

        def register_on_blockchain(url):
            try:
                response = requests.post(
                    f"{url}/register_asset",
                    json=registration_data,
                    timeout=5
                )
                if response.status_code == 200:
                    result = response.json()
                    if result.get("success"):
                        logger.info(f"Successfully registered on {url}: {result.get('result')}")
                        return (True, url, result.get("result"))
                    else:
                        logger.warning(f"Registration failed on {url}: {result.get('message')}")
                        return (False, url, result.get("message"))
                else:
                    logger.warning(f"Registration failed on {url}, status: {response.status_code}")
                    return (False, url, f"HTTP {response.status_code}")
            except Exception as e:
                logger.error(f"Error registering on {url}: {str(e)}")
                return (False, url, str(e))

        futures = [self.executor.submit(register_on_blockchain, url) for url in selected_urls]
        for future in as_completed(futures):
            success, url, result = future.result()
            if success:
                successes.append(url)
                node_ids.append(result)

        success_count = len(successes)
        if success_count >= self.min_consensus:
            logger.info(f"Asset {asset_id} registered with consensus ({success_count}/{target_count})")
            return True, f"Asset registered with consensus ({success_count}/{target_count})", node_ids
        else:
            logger.warning(f"Failed to reach consensus for asset {asset_id} ({success_count}/{self.min_consensus})")
            self._cleanup_registrations(asset_id, successes)
            return False, f"Failed to reach consensus ({success_count}/{self.min_consensus})", []

    def _cleanup_registrations(self, asset_id: str, urls: List[str]):

        logger.info(f"Cleanup needed for asset {asset_id} on {len(urls)} blockchain instances")

    def transfer_asset(self, asset_id: str, from_user_id: str, to_user_id: str) -> Tuple[bool, str, List[str]]:

        if len(self.active_urls) < self.min_consensus:
            return False, f"Not enough active blockchain instances ({len(self.active_urls)}/{self.min_consensus})", []

        self.active_urls = self._check_active_blockchains()

        blockchains_with_asset = self._find_blockchains_with_asset(asset_id)

        if len(blockchains_with_asset) < self.min_consensus:
            valid_blockchains = []

            for url in blockchains_with_asset:
                if self._verify_ownership(url, asset_id, from_user_id):
                    valid_blockchains.append(url)

            if len(valid_blockchains) > 0:
                logger.info(f"Asset {asset_id} found on {len(valid_blockchains)} blockchains, "
                            f"but below consensus threshold ({self.min_consensus})")

                self._replicate_asset(asset_id, from_user_id, valid_blockchains)

                blockchains_with_asset = self._find_blockchains_with_asset(asset_id)
            else:
                return False, f"Asset {asset_id} not owned by {from_user_id} on any blockchain", []

        valid_blockchains = []
        for url in blockchains_with_asset:
            if self._verify_ownership(url, asset_id, from_user_id):
                valid_blockchains.append(url)

        if len(valid_blockchains) < self.min_consensus:
            return False, (f"Ownership verification failed: Asset {asset_id} is not owned by {from_user_id} "
                           f"on enough blockchains ({len(valid_blockchains)}/{self.min_consensus})"), []

        transfer_data = {
            "asset_id": asset_id,
            "from_user_id": from_user_id,
            "to_user_id": to_user_id
        }

        successes = []
        node_ids = []

        def transfer_on_blockchain(url):
            try:
                response = requests.post(
                    f"{url}/transfer_asset",
                    json=transfer_data,
                    timeout=5
                )
                if response.status_code == 200:
                    result = response.json()
                    if result.get("success"):
                        logger.info(f"Successfully transferred on {url}: {result.get('result')}")
                        return (True, url, result.get("result"))
                    else:
                        logger.warning(f"Transfer failed on {url}: {result.get('result', 'unknown')}")
                        return (False, url, result.get("result", "unknown"))
                else:
                    logger.warning(f"Transfer failed on {url}, status: {response.status_code}")
                    return (False, url, f"HTTP {response.status_code}")
            except Exception as e:
                logger.error(f"Error transferring on {url}: {str(e)}")
                return (False, url, str(e))

        futures = [self.executor.submit(transfer_on_blockchain, url) for url in valid_blockchains]
        for future in as_completed(futures):
            success, url, result = future.result()
            if success:
                successes.append(url)
                node_ids.append(result)

        success_count = len(successes)
        if success_count >= self.min_consensus:
            logger.info(f"Asset {asset_id} transferred with consensus ({success_count}/{len(valid_blockchains)})")
            return True, f"Asset transferred with consensus ({success_count}/{len(valid_blockchains)})", node_ids
        else:
            logger.warning(f"Failed to reach consensus for transfer of asset {asset_id} "
                           f"({success_count}/{self.min_consensus})")
            return False, f"Transfer failed to reach consensus ({success_count}/{self.min_consensus})", []

    def _find_blockchains_with_asset(self, asset_id: str) -> List[str]:

        blockchains_with_asset = []

        def check_asset(url):
            try:
                response = requests.get(f"{url}/asset_history/{asset_id}", timeout=2)
                if response.status_code == 200:
                    result = response.json()
                    history = result.get("history", [])
                    if history:
                        return url
            except Exception as e:
                logger.debug(f"Error checking asset on {url}: {str(e)}")
            return None

        futures = [self.executor.submit(check_asset, url) for url in self.active_urls]
        for future in as_completed(futures):
            result = future.result()
            if result:
                blockchains_with_asset.append(result)

        return blockchains_with_asset

    def _verify_ownership(self, url: str, asset_id: str, user_id: str) -> bool:

        try:
            response = requests.get(
                f"{url}/verify_ownership",
                params={"asset_id": asset_id, "user_id": user_id},
                timeout=2
            )
            if response.status_code == 200:
                result = response.json()
                return result.get("is_owner", False)
        except Exception as e:
            logger.warning(f"Error verifying ownership on {url}: {str(e)}")
        return False

    def _replicate_asset(self, asset_id: str, user_id: str, source_blockchains: List[str]):

        if not source_blockchains:
            logger.warning(f"Cannot replicate asset {asset_id} - no source blockchains provided")
            return

        asset_data = self._get_asset_data(source_blockchains[0], asset_id)
        if not asset_data:
            logger.warning(f"Failed to get asset data for replication: {asset_id}")
            return

        target_blockchain_count = self.min_consensus
        needed_count = target_blockchain_count - len(source_blockchains)

        if needed_count <= 0:
            logger.info(f"Asset {asset_id} already exists on enough blockchains")
            return

        candidates = [url for url in self.active_urls if url not in source_blockchains]

        if len(candidates) < needed_count:
            logger.warning(f"Not enough available blockchains for replication: "
                          f"need {needed_count}, found {len(candidates)}")
            return

        target_blockchains = random.sample(candidates, needed_count)

        logger.info(f"Replicating asset {asset_id} to {needed_count} more blockchains")

        registration_data = {
            "asset_id": asset_id,
            "user_id": user_id,
            "asset_data": asset_data
        }

        successes = []

        def register_on_blockchain(url):
            try:
                response = requests.post(
                    f"{url}/register_asset",
                    json=registration_data,
                    timeout=5
                )
                if response.status_code == 200:
                    result = response.json()
                    if result.get("success"):
                        logger.info(f"Successfully replicated on {url}: {result.get('result')}")
                        return (True, url)
                    else:
                        logger.warning(f"Replication failed on {url}: {result.get('message')}")
                        return (False, url)
                else:
                    logger.warning(f"Replication failed on {url}, status: {response.status_code}")
                    return (False, url)
            except Exception as e:
                logger.error(f"Error replicating on {url}: {str(e)}")
                return (False, url)

        futures = [self.executor.submit(register_on_blockchain, url) for url in target_blockchains]
        for future in as_completed(futures):
            success, url = future.result()
            if success:
                successes.append(url)

        logger.info(f"Replicated asset {asset_id} to {len(successes)}/{needed_count} additional blockchains")

    def _get_asset_data(self, url: str, asset_id: str) -> Dict[str, Any]:

        try:
            response = requests.get(f"{url}/asset_data/{asset_id}", timeout=2)
            if response.status_code == 200:
                result = response.json()
                return result.get("data", {})
        except Exception as e:
            logger.warning(f"Error getting asset data from {url}: {str(e)}")
        return {}

    def get_asset_data(self, asset_id: str) -> Dict[str, Any]:

        self.active_urls = self._check_active_blockchains()

        blockchains_with_asset = self._find_blockchains_with_asset(asset_id)

        if len(blockchains_with_asset) < self.min_consensus:
            logger.warning(f"Asset {asset_id} not found on enough blockchains ({len(blockchains_with_asset)}/{self.min_consensus})")
            return {}

        asset_data_list = []

        def get_data_from_blockchain(url):
            return self._get_asset_data(url, asset_id)

        futures = [self.executor.submit(get_data_from_blockchain, url) for url in blockchains_with_asset]
        for future in as_completed(futures):
            data = future.result()
            if data:
                asset_data_list.append(data)

        if len(asset_data_list) < self.min_consensus:
            logger.warning(f"Could not get asset data with consensus for {asset_id} ({len(asset_data_list)}/{self.min_consensus})")
            return {}

        return asset_data_list[0]

    def _get_asset_history(self, url: str, asset_id: str) -> List[Dict[str, Any]]:

        try:
            response = requests.get(f"{url}/asset_history/{asset_id}", timeout=2)
            if response.status_code == 200:
                result = response.json()
                return result.get("history", [])
        except Exception as e:
            logger.warning(f"Error getting asset history from {url}: {str(e)}")
        return []

    def get_asset_history(self, asset_id: str) -> List[Dict[str, Any]]:

        self.active_urls = self._check_active_blockchains()

        blockchains_with_asset = self._find_blockchains_with_asset(asset_id)

        if len(blockchains_with_asset) < self.min_consensus:
            logger.warning(f"Asset {asset_id} not found on enough blockchains ({len(blockchains_with_asset)}/{self.min_consensus})")
            return []

        history_list = []

        def get_history_from_blockchain(url):
            return self._get_asset_history(url, asset_id)

        futures = [self.executor.submit(get_history_from_blockchain, url) for url in blockchains_with_asset]
        for future in as_completed(futures):
            history = future.result()
            if history:
                history_list.append(history)

        if len(history_list) < self.min_consensus:
            logger.warning(f"Could not get asset history with consensus for {asset_id} ({len(history_list)}/{self.min_consensus})")
            return []

        return history_list[0]

    def stake_asset(self, asset_id: str, user_id: str, staking_amount: int = 2400) -> Tuple[bool, str, List[str]]:
        return False, "Staking functionality has been removed", []

    def get_user_balance(self, user_id: str) -> int:
        # User balance functionality simplified - always returns 0 as staking is removed
        logger.info(f"User {user_id} balance request - returning 0 (staking functionality removed)")
        return 0

    def get_user_assets(self, user_id: str) -> List[str]:

        self.active_urls = self._check_active_blockchains()

        all_assets = set()

        def get_assets_from_blockchain(url):
            try:
                response = requests.get(f"{url}/user_assets/{user_id}", timeout=2)
                if response.status_code == 200:
                    result = response.json()
                    return result.get("assets", [])
            except Exception as e:
                logger.debug(f"Error getting assets from {url}: {str(e)}")
            return []

        futures = [self.executor.submit(get_assets_from_blockchain, url) for url in self.active_urls]
        for future in as_completed(futures):
            assets = future.result()
            all_assets.update(assets)

        logger.info(f"User {user_id} has {len(all_assets)} unique assets across all blockchains")
        return list(all_assets)

    def get_asset_staking_status(self, asset_id: str) -> Optional[Dict[str, Any]]:
        self.active_urls = self._check_active_blockchains()
        
        blockchains_with_asset = self._find_blockchains_with_asset(asset_id)
        
        if not blockchains_with_asset:
            logger.warning(f"Asset {asset_id} not found on any blockchain")
            return None
            
        # Try to get the current owner
        try:
            history = self._get_asset_history(blockchains_with_asset[0], asset_id)
            if history:
                current_owner = history[-1].get("user_id")
                return {
                    "is_staked": False,
                    "owner_id": current_owner
                }
        except Exception as e:
            logger.debug(f"Error getting asset history: {str(e)}")
            
        return {
            "is_staked": False,
            "owner_id": None
        }


app = Flask(__name__)
orchestrator = BlockchainOrchestrator()

@app.route('/health', methods=['GET'])
def health_check():
    return jsonify({
        "status": "ok",
        "service": "InLock Blockchain Orchestrator",
        "active_blockchains": len(orchestrator.active_urls),
        "min_consensus": orchestrator.min_consensus
    })

@app.route('/register_asset', methods=['POST'])
def api_register_asset():
    try:
        data = request.json
        asset_id = data.get('asset_id')
        user_id = data.get('user_id')
        asset_data = data.get('asset_data', {})

        if not asset_id or not user_id:
            return jsonify({"success": False, "message": "Missing required fields"}), 400

        success, message, node_ids = orchestrator.register_asset(asset_id, user_id, asset_data)

        return jsonify({
            "success": success,
            "message": message,
            "node_ids": node_ids
        })
    except Exception as e:
        logger.error(f"Error in register_asset: {str(e)}", exc_info=True)
        return jsonify({"success": False, "message": f"Server error: {str(e)}"}), 500

@app.route('/transfer_asset', methods=['POST'])
def api_transfer_asset():
    try:
        logger.info("🔄 ORCHESTRATOR: Received transfer_asset request")
        data = request.json
        asset_id = data.get('asset_id')
        from_user_id = data.get('from_user_id')
        to_user_id = data.get('to_user_id')

        logger.info(f"🔄 ORCHESTRATOR: Transfer request details - Asset: {asset_id}, From: {from_user_id}, To: {to_user_id}")

        if not asset_id or not from_user_id or not to_user_id:
            logger.error(f"❌ ORCHESTRATOR ERROR: Missing required fields in transfer request")
            return jsonify({"success": False, "message": "Missing required fields"}), 400

        active_blockchains = len(orchestrator.active_urls)
        logger.info(f"🔄 ORCHESTRATOR: Active blockchains before transfer: {active_blockchains}")

        try:
            blockchains_with_asset = orchestrator._find_blockchains_with_asset(asset_id)
            logger.info(f"🔄 ORCHESTRATOR: Asset found on {len(blockchains_with_asset)} blockchains")

            if len(blockchains_with_asset) == 0:
                logger.error(f"❌ ORCHESTRATOR ERROR: Asset {asset_id} not found on any blockchain")
                return jsonify({
                    "success": False,
                    "message": f"Asset {asset_id} not found on any blockchain",
                    "node_ids": []
                })

            valid_blockchains = []
            for url in blockchains_with_asset:
                if orchestrator._verify_ownership(url, asset_id, from_user_id):
                    valid_blockchains.append(url)

            logger.info(f"🔄 ORCHESTRATOR: Ownership verified on {len(valid_blockchains)}/{len(blockchains_with_asset)} blockchains")

            if len(valid_blockchains) < orchestrator.min_consensus:
                logger.error(f"❌ ORCHESTRATOR ERROR: Insufficient ownership verification ({len(valid_blockchains)}/{orchestrator.min_consensus})")
                return jsonify({
                    "success": False,
                    "message": f"Ownership verification failed: {from_user_id} is not the owner on enough blockchains",
                    "node_ids": []
                })
        except Exception as verify_err:
            logger.error(f"❌ ORCHESTRATOR ERROR: Error during ownership verification: {str(verify_err)}", exc_info=True)

        logger.info(f"🔄 ORCHESTRATOR: Executing transfer_asset operation")
        success, message, node_ids = orchestrator.transfer_asset(asset_id, from_user_id, to_user_id)

        logger.info(f"{'✅' if success else '❌'} ORCHESTRATOR: Transfer result - Success: {success}, Message: {message}, Nodes: {node_ids}")

        response_data = {
            "success": success,
            "message": message,
            "node_ids": node_ids
        }

        logger.info(f"🔄 ORCHESTRATOR: Sending response: {response_data}")

        return jsonify(response_data)
    except Exception as e:
        logger.error(f"❌ ORCHESTRATOR ERROR: Unhandled exception in transfer_asset: {str(e)}", exc_info=True)
        return jsonify({"success": False, "message": f"Server error: {str(e)}"}), 500

@app.route('/stake_asset', methods=['POST'])
def api_stake_asset():
    return jsonify({
        "success": False,
        "message": "Staking functionality has been removed",
        "node_ids": []
    })

@app.route('/user_balance/<user_id>', methods=['GET'])
def api_user_balance(user_id):
    return jsonify({"user_id": user_id, "balance": 0, "message": "Staking functionality has been removed"})

@app.route('/user_assets/<user_id>', methods=['GET'])
def api_user_assets(user_id):
    try:
        assets = orchestrator.get_user_assets(user_id)
        return jsonify({"user_id": user_id, "assets": assets})
    except Exception as e:
        logger.error(f"Error in user_assets: {str(e)}", exc_info=True)
        return jsonify({"success": False, "message": f"Server error: {str(e)}"}), 500

@app.route('/asset_staking_status/<asset_id>', methods=['GET'])
def api_asset_staking_status(asset_id):
    try:
        status = orchestrator.get_asset_staking_status(asset_id)
        if status:
            return jsonify({
                "success": True,
                "asset_id": asset_id,
                "staking_status": {
                    "is_staked": False,
                    "owner_id": status.get("owner_id")
                }
            })
        else:
            return jsonify({
                "success": False,
                "asset_id": asset_id,
                "message": "Asset not found"
            })
    except Exception as e:
        logger.error(f"Error in asset_staking_status: {str(e)}", exc_info=True)
        return jsonify({"success": False, "message": f"Server error: {str(e)}"}), 500

@app.route('/asset_data/<asset_id>', methods=['GET'])
def api_asset_data(asset_id):
    try:
        data = orchestrator.get_asset_data(asset_id)
        return jsonify({"asset_id": asset_id, "data": data})
    except Exception as e:
        logger.error(f"Error in asset_data: {str(e)}", exc_info=True)
        return jsonify({"success": False, "message": f"Server error: {str(e)}"}), 500

@app.route('/asset_history/<asset_id>', methods=['GET'])
def api_asset_history(asset_id):
    try:
        history = orchestrator.get_asset_history(asset_id)
        return jsonify({"asset_id": asset_id, "history": history})
    except Exception as e:
        logger.error(f"Error in asset_history: {str(e)}", exc_info=True)
        return jsonify({"success": False, "message": f"Server error: {str(e)}"}), 500

@app.route('/verify_ownership', methods=['GET'])
def api_verify_ownership():
    try:
        asset_id = request.args.get('asset_id')
        user_id = request.args.get('user_id')

        if not asset_id or not user_id:
            return jsonify({"success": False, "message": "Missing required parameters"}), 400

        blockchains_with_asset = orchestrator._find_blockchains_with_asset(asset_id)

        if not blockchains_with_asset:
            return jsonify({
                "success": True,
                "asset_id": asset_id,
                "user_id": user_id,
                "is_owner": False,
                "message": "Asset not found on any blockchain"
            })

        verified_count = 0
        for url in blockchains_with_asset:
            if orchestrator._verify_ownership(url, asset_id, user_id):
                verified_count += 1

        is_owner = verified_count >= orchestrator.min_consensus

        return jsonify({
            "success": True,
            "asset_id": asset_id,
            "user_id": user_id,
            "is_owner": is_owner,
            "verified_count": verified_count,
            "total_blockchains": len(blockchains_with_asset),
            "min_consensus": orchestrator.min_consensus
        })
    except Exception as e:
        logger.error(f"Error in verify_ownership: {str(e)}", exc_info=True)
        return jsonify({"success": False, "message": f"Server error: {str(e)}"}), 500

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=6000)