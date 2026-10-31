#!/bin/bash
# Main menu for Group 1's OSSP File Management application.
# Usage: ./FileManager.sh [working-directory]

cd "$(dirname "$0")" || exit 1
BUILD_DIR="build"
WORK_DIR="${1:-$PWD}"

# Compile the Java sources if the build is missing or out of date.
build() {
    if [[ ! -f "$BUILD_DIR/FileManager.class" ]] || [[ -n "$(find . -maxdepth 1 -name '*.java' -newer "$BUILD_DIR/FileManager.class")" ]]; then
        echo "Compiling..."
        mkdir -p "$BUILD_DIR"
        javac -d "$BUILD_DIR" ./*.java || { echo "Error: compilation failed."; return 1; }
    fi
}

echo "Welcome to Group 1's OSSP File Management application!"

while true; do
    echo
    echo "Please type the corresponding number below to select an option:"
    echo "[1] Login"
    echo "[2] Create account"
    echo "[3] Exit"

    if ! read -r userchoice; then
        exit 0
    fi

    case "$userchoice" in
        1)
            build && java -cp "$BUILD_DIR" FileManager "$WORK_DIR"
            ;;
        2)
            echo "Account creation is not implemented yet." #TODO: ADD CODE TO LAUNCH ACCOUNT CREATION
            ;;
        3)
            echo "The file manager will now exit, goodbye!"
            exit 0
            ;;
        *)
            echo "Error: Invalid input, please enter 1 to login, 2 to create an account, 3 to exit the program."
            ;;
    esac
done
