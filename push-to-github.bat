@echo off
REM Script para subir el proyecto a GitHub

echo ========================================
echo Subiendo proyecto a GitHub...
echo ========================================

REM Configura la URL de tu repositorio de GitHub aquí:
set REPO_URL=https://github.com/TGabVoid/Voided-dimension-MinecraftMod-1.21.git

echo Verificando repositorio remoto...
git remote get-url origin >nul 2>&1
if errorlevel 1 (
  echo Agregando repositorio remoto...
  git remote add origin %REPO_URL%
) else (
  echo Actualizando URL del repositorio remoto...
  git remote set-url origin %REPO_URL%
)

echo.
echo Agregando todos los cambios...
git add -A

echo.
echo Creando commit con los cambios...
git commit -m "Actualización: subiendo proyecto completo a GitHub"

echo.
echo Cambiando la rama a 'main'...
git branch -M main

echo.
echo Subiendo archivos a GitHub...
git push -u origin main

echo.
echo ========================================
echo Proceso completado!
echo ========================================
echo Tu proyecto ahora está en GitHub
pause

