@echo off
echo Assigning drive letter D: to bootfs partition...
diskpart /s "G:\projects\ai_project\ai_manager\diskpart_script.txt"
echo Done! Check if D: drive appears in File Explorer.
pause
