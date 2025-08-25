# playersoldinfake

Фейковые игроки для TAB и чата (Spigot/Paper 1.16–1.21.x). Требуется **ProtocolLib** для отображения в TAB.

## Возможности
- Фейки появляются в TAB (минимум `min-players`, максимум `max-players`).
- Имитация join/leave в чате.
- Случайные сообщения в чат.
- Команды `/playersoldinfake` (`/psf`):
  - `reload`
  - `add <ник>`
  - `remove <ник>`
  - `removeall`
  - `addrandom <кол-во>`

## Зависимость
- [ProtocolLib] установите на сервер (как плагин).

## Сборка
```bash
mvn package
```
JAR будет в `target/playersoldinfake-1.0.0.jar`.

## Конфиг
Смотри `config.yml`. Все 15 (и больше) ников можно редактировать.
