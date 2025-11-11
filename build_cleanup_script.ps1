./gradlew --stop
Remove-Item -Recurse -Force .\build
Remove-Item -Recurse -Force .\out
Remove-Item -Recurse -Force ".\.gradle"
./gradlew build --info