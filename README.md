# GlowSalary

Высокопроизводительный и надежный плагин системы зарплат для серверов **Paper** и **Folia** (Minecraft 1.21+, Java 21).

## Особенности

- **Прогрессивная шкала выплат**: размер зарплаты растет с каждым успешным получением вплоть до настраиваемого лимита.
- **Поддержка LuckPerms**: гибкая настройка сумм, кулдаунов и прогрессии индивидуально для каждой группы привилегий. Автоматический выбор группы с наивысшим весом.
- **Редкая зарплата в сапфирах**: настраиваемый шанс выпадения особой валюты (сапфиров) вместо денег. Количество сапфиров также увеличивается с прогрессом игрока.
- **Информативный кулдаун**: при попытке получить зарплату во время перезарядки плагин сообщает точное оставшееся время до следующей выплаты и её будущий размер.
- **Поддержка Folia**: полная потокобезопасность и совместимость с региональным шедулингом Folia и классическим Paper.
- **SQLite + HikariCP**: транзакционное и асинхронное сохранение данных без просадок TPS и фризов основного потока.
- **Adventure MiniMessage**: современные визуальные сообщения с поддержкой градиентов, тегов и правильных русских склонений.

---

## Команды и права

| Команда | Описание | Право | По умолчанию |
|---|---|---|---|
| `/salary` (алиас: `/glowsalary`) | Забрать зарплату или узнать время до следующей | `glowsalary.use` | Всем игрокам |
| `/salary info [игрок]` | Просмотреть информацию о стрике и статистике выплат | `glowsalary.use` (себе) / `glowsalary.admin` (другим) | Всем / OP |
| `/salary reload` | Перезагрузить конфигурацию и сообщения | `glowsalary.admin` | OP |
| `/salary reset <игрок>` | Сбросить прогресс и кулдаун игрока | `glowsalary.admin` | OP |

---

## Настройка (`config.yml`)

```yaml
config-version: 1
default-cooldown-seconds: 7200
auto-save-interval-minutes: 5

sapphire:
  enabled: true
  chance-percent: 15.0
  base-amount: 1
  increment-per-claim: 1
  max-amount: 10
  sound: "entity.player.levelup"
  reward-commands:
    - 'minecraft:give <player> minecraft:lapis_lazuli[minecraft:custom_name=''{"text":"Сапфир","color":"#38bdf8","bold":true}'',minecraft:lore=[''{"text":"Редкая валюта сервера","color":"gray"}'']] <amount>'

groups:
  default:
    base-salary: 150.0
    increment-per-claim: 15.0
    max-salary: 600.0
    cooldown-seconds: 7200
  vip:
    base-salary: 300.0
    increment-per-claim: 30.0
    max-salary: 1200.0
    cooldown-seconds: 5400
  premium:
    base-salary: 500.0
    increment-per-claim: 50.0
    max-salary: 2500.0
    cooldown-seconds: 3600
  deluxe:
    base-salary: 1000.0
    increment-per-claim: 100.0
    max-salary: 5000.0
    cooldown-seconds: 1800
```

---

## Сборка из исходников

Требуется **Java 21** и **Maven 3.9+**:

```bash
mvn clean package
```

Собранный `.jar` файл будет доступен в папке `target/GlowSalary-1.0.4.jar`.
