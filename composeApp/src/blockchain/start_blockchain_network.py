import os
import sys
import subprocess
import time
import socket
import argparse
import signal
import threading
import logging
from typing import List, Dict

logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger('blockchain_network')

BASE_PORT = 5001

processes = []

def is_port_in_use(port: int) -> bool:

    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
        return s.connect_ex(('localhost', port)) == 0

def start_blockchain_node(port: int, storage_path: str) -> subprocess.Popen:

    if is_port_in_use(port):
        logger.warning(f"Port {port} is already in use, skipping this node")
        return None

    env = os.environ.copy()

    command = [
        sys.executable,
        "blockchain.py",
        "--port", str(port),
        "--storage", storage_path
    ]

    logger.info(f"Starting blockchain node on port {port} with storage {storage_path}")

    process = subprocess.Popen(
        command,
        env=env,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        text=True
    )

    time.sleep(1)

    if process.poll() is not None:
        stdout, stderr = process.communicate()
        logger.error(f"Failed to start blockchain node on port {port}")
        logger.error(f"STDOUT: {stdout}")
        logger.error(f"STDERR: {stderr}")
        return None

    return process

def start_orchestrator() -> subprocess.Popen:

    if is_port_in_use(6000):
        logger.warning("Port 6000 is already in use, skipping orchestrator")
        return None

    env = os.environ.copy()

    command = [
        sys.executable,
        "orchestrator.py"
    ]

    logger.info("Starting blockchain orchestrator on port 6000")

    process = subprocess.Popen(
        command,
        env=env,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        text=True
    )

    time.sleep(1)

    if process.poll() is not None:
        stdout, stderr = process.communicate()
        logger.error("Failed to start blockchain orchestrator")
        logger.error(f"STDOUT: {stdout}")
        logger.error(f"STDERR: {stderr}")
        return None

    return process

def log_output(process: subprocess.Popen, name: str):

    def read_and_log_output(pipe, prefix):
        for line in iter(pipe.readline, ''):
            logger.info(f"{prefix}: {line.strip()}")

    stdout_thread = threading.Thread(
        target=read_and_log_output,
        args=(process.stdout, f"{name} - STDOUT"),
        daemon=True
    )
    stderr_thread = threading.Thread(
        target=read_and_log_output,
        args=(process.stderr, f"{name} - STDERR"),
        daemon=True
    )

    stdout_thread.start()
    stderr_thread.start()

def create_data_directories(node_count: int) -> List[str]:

    paths = []

    data_dir = os.path.abspath("blockchain_data")
    if not os.path.exists(data_dir):
        os.makedirs(data_dir)

    for i in range(node_count):
        node_dir = os.path.join(data_dir, f"node_{i + 1}")
        if not os.path.exists(node_dir):
            os.makedirs(node_dir)
        paths.append(os.path.join(node_dir, "blockchain_dag.json"))

    return paths

def signal_handler(sig, frame):

    logger.info("Shutting down blockchain network...")
    for process in processes:
        if process and process.poll() is None:
            process.terminate()

    for process in processes:
        if process:
            try:
                process.wait(timeout=5)
            except subprocess.TimeoutExpired:
                process.kill()

    logger.info("Blockchain network shutdown complete")
    sys.exit(0)

def main():

    parser = argparse.ArgumentParser(description='Start a blockchain network with multiple nodes')
    parser.add_argument('-n', '--nodes', type=int, default=7, help='Number of blockchain nodes to start')
    args = parser.parse_args()

    signal.signal(signal.SIGINT, signal_handler)
    signal.signal(signal.SIGTERM, signal_handler)

    storage_paths = create_data_directories(args.nodes)

    for i in range(args.nodes):
        port = BASE_PORT + i
        process = start_blockchain_node(port, storage_paths[i])
        if process:
            processes.append(process)
            log_output(process, f"Blockchain-{port}")

    orchestrator_process = start_orchestrator()
    if orchestrator_process:
        processes.append(orchestrator_process)
        log_output(orchestrator_process, "Orchestrator")

    active_nodes = [p for p in processes if p and p.poll() is None]
    logger.info(f"Blockchain network started with {len(active_nodes) - 1} nodes + orchestrator")
    logger.info(f"Orchestrator API endpoint: http://localhost:6000")

    try:
        while True:
            time.sleep(1)

            for i, process in enumerate(processes):
                if process and process.poll() is not None:
                    if i == len(processes) - 1:
                        logger.error("Orchestrator has terminated unexpectedly!")
                    else:
                        logger.error(f"Blockchain node on port {BASE_PORT + i} has terminated unexpectedly!")

            active_nodes = [p for p in processes if p and p.poll() is None]
            if len(active_nodes) == 0:
                logger.error("All processes have terminated. Exiting.")
                break
    except KeyboardInterrupt:
        pass
    finally:
        signal_handler(None, None)

if __name__ == "__main__":
    main()