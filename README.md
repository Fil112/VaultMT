# 💎 VaultMT

<div align="center">

![GitHub version](https://img.shields.io/badge/version-1.8--26.2-blue.svg?style=for-the-badge)
![Java Version](https://img.shields.io/badge/java-8%2B-orange.svg?style=for-the-badge)
![License](https://img.shields.io/badge/license-MT-blue.svg?style=for-the-badge)

<br>

![Folia](https://img.shields.io/badge/Folia-Supported-purple.svg?style=flat-square)
![Sponge](https://img.shields.io/badge/Sponge-Supported-purple.svg?style=flat-square)
![Velocity](https://img.shields.io/badge/Velocity-Supported-purple.svg?style=flat-square)
![BungeeCord](https://img.shields.io/badge/BungeeCord-Supported-purple.svg?style=flat-square)
![Bukkit](https://img.shields.io/badge/Bukkit-Supported-purple.svg?style=flat-square)

> **VaultMT** — экономика, которая работает на тебя. Быстрая, современная и независимая.

</div>

---

**VaultMT** — это высокопроизводительное, модульное решение для экономики серверов Minecraft. Мы отказались от монолитных устаревших систем в пользу архитектуры, ориентированной на гибкость, скорость и полную интеграцию с современными ядрами (включая **Folia**).

## ✨ Почему выбирают VaultMT?

* ⚡ **Исключительная производительность:** Минимальное влияние на TPS сервера. Оптимизировано для работы на актуальных версиях Java.
* 🌉 **Архитектура Bridge:** Уникальная система независимых провайдеров (SQLite/MySQL) для создания глобальной экономики на всей сети серверов.
* 🧬 **Полная поддержка Folia:** Native-поддержка многопоточной среды без лагов и рассинхронов.
* ⚙️ **Гибкий конфиг:** Полный контроль над финансами — от динамического налога на переводы до продвинутой мультивалютности.
* 💻 **Developer-Friendly:** Чистое, современное и полностью асинхронное API для интеграции в ваши собственные плагины.

---

## 🚀 Инструкция по установке

Установка и запуск плагина занимают меньше минуты:

1. 📥 **Скачайте** актуальную версию `VaultMT.jar` со страницы релизов.
2. 📂 **Переместите** скачанный файл в папку `plugins/` вашего сервера (Bukkit, BungeeCord или Velocity).
3. 🔄 **Запустите** (или перезагрузите) сервер. Плагин автоматически сгенерирует папку `VaultMT` с файлом `config.yml`.
4. ✏️ **Настройте** базу данных и валюту в `config.yml` под ваши нужды. *(Для связки нескольких серверов обязательно используйте тип БД `mysql`)*.
5. ✅ Введите в консоли команду `/emt reload`, чтобы применить изменения. **Готово!**

---

## ⌨️ Команды и права

| Команда | Описание | Права (Permission) |
| :--- | :--- | :--- |
| `/balance [игрок]` | Узнать свой баланс или баланс другого игрока | `vaultmt.balance` *(Всем)* |
| `/pay <игрок> <сумма>` | Перевести деньги другому игроку | `vaultmt.pay` *(Всем)* |
| `/emt stats` | Показать экономическую статистику сервера | `vaultmt.admin` |
| `/emt give <игрок> <сумма>`| Выдать монеты игроку | `vaultmt.admin` |
| `/emt take <игрок> <сумма>`| Забрать монеты у игрока | `vaultmt.admin` |
| `/emt set <игрок> <сумма>` | Установить точный баланс игроку | `vaultmt.admin` |
| `/emt log <игрок>` | Посмотреть историю транзакций игрока | `vaultmt.admin` |
| `/emt version` | Показать версию плагина | `vaultmt.admin` |
| `/emt reload` | Перезагрузить конфигурацию плагина | `vaultmt.admin` |

---

## 🧩 Плейсхолдеры (PlaceholderAPI)

Для работы плейсхолдеров убедитесь, что на сервере установлен плагин **PlaceholderAPI**, а в `config.yml` включен параметр `enable-papi: true`.

| Плейсхолдер | Результат | Пример вывода |
| :--- | :--- | :--- |
| `%vaultmt_balance%` | Числовое значение текущего баланса | `1050.5` |
| `%vaultmt_formatted%`| Баланс с настроенным форматом и символом валюты | `1050.50 $` |
| `%vaultmt_fee%` | Текущая базовая комиссия на переводы в процентах | `5.0%` |

---

## 💻 Для разработчиков (API)

VaultMT предоставляет независимое, полностью асинхронное API для легкой интеграции экономики в ваши плагины и ботов.

### 1. Подключение через Maven (JitPack)
Добавьте репозиторий и зависимость в ваш файл `pom.xml`:

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>[https://jitpack.io](https://jitpack.io)</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>com.github.Fil112</groupId>
        <artifactId>VaultMT</artifactId>
        <version>1.1.0</version>
        <scope>provided</scope>
    </dependency>
</dependencies>
```

2. Использование API в коде
Вся работа с финансами осуществляется через интерфейс EconomyProvider. Мы настоятельно рекомендуем использовать методы с суффиксом Async, чтобы не создавать задержек (фризов) на основном потоке сервера при чтении из баз данных.
Java
```java
import mt.vault.api.VaultMT;
import mt.vault.api.EconomyProvider;
import mt.vault.api.TransactionResult;
import org.bukkit.entity.Player;

public class MyCustomPlugin {
    
    public void rewardPlayer(Player player) {
        // 1. Проверяем, инициализировано ли ядро экономики
        if (!VaultMT.hasProvider()) {
            return;
        }

        // 2. Получаем активный провайдер (SQLite / MySQL)
        EconomyProvider economy = VaultMT.getProvider();

        // 3. Выполняем асинхронное начисление средств
        economy.depositAsync(player.getUniqueId(), 500.0, "mt", "Награда за квест")
            .thenAccept(result -> {
                
                // Обработка результата происходит после ответа от базы данных
                if (result.isSuccess()) {
                    player.sendMessage("§aКвест выполнен! Получено 500$.");
                    player.sendMessage("§7Ваш новый баланс: " + result.balance());
                } else {
                    player.sendMessage("§cОшибка транзакции: " + result.errorMessage());
                }
                
            });
    }
}
```


🤝 Поддержка и развитие

VaultMT — это открытый проект. Мы приветствуем предложения по улучшению функционала и сообщения об ошибках (issues). Если вы разработчик и хотите внедрить более сложные механики с VaultMT в свой проект, присоединяйтесь к развитию экосистемы MT Studio!