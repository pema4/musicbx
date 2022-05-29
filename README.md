# О проекте

Визуальный редактор для прототипирования аудио-эффектов
Интерфейс написан на Compose Desktop, вся обработка звука - на Rust
(с использованием `cpal`, `glicol_synth`).
В теории все части проекта кроссплатформенные,
но я тестировал приложение только на macOS с Apple Silicon

## Сборка проекта

Проект собирался на macOS с Apple Silicon,
на других ОС работоспособность не могу гарантировать

Для сборки нужен установленный `cargo` и `rustup` (cборка проекта проверялась на Rust 1.61)
```shell
brew install gradle rustup-init
rustup-init -y
```

Далее проект можно запустить из директории `editor`
```shell
cd ./editor
gradle run
```

Для запуска приложения подходит Eclipse Temurin JDK17
С OpenJDK приложение не работает, с остальными сборками не пробовал
Если необходимо, можно указать правильный JAVA_HOME для gradle

Ещё можно собрать .dmg образ приложения со встроенным JRE:
```shell
gradle packageDmg
open build/compose/binaries/main/dmg/editor-1.0.0.dmg
```
