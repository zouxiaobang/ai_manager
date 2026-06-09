from ai_manager_daemon.config import load_config
from ai_manager_daemon.logging_setup import setup_logging
from ai_manager_daemon.runtime_state import register_pid_cleanup
from ai_manager_daemon.server import run_server


def main() -> None:
    setup_logging()
    register_pid_cleanup()
    config = load_config()
    run_server(config)


if __name__ == "__main__":
    main()
