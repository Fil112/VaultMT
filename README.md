# VaultMT

<div align="center">

![GitHub version](https://img.shields.io/badge/version-1.8--26.2-blue.svg)
![Java Version](https://img.shields.io/badge/java-8%2B-orange.svg)
![License](https://img.shields.io/badge/license-MT-blue.svg)

![Folia](https://img.shields.io/badge/Folia-Supported-purple.svg)
![Sponge](https://img.shields.io/badge/Sponge-Supported-purple.svg)
![Velocity](https://img.shields.io/badge/Velocity-Supported-purple.svg)
![BumgeeCord](https://img.shields.io/badge/BungeeCord-Supported-purple.svg)
![Bukkit](https://img.shields.io/badge/Bukkit-Supported-purple.svg)

VaultMT — экономика, которая работает на тебя.

</div>

---

VaultMT — это высокопроизводительное, модульное решение для экономики серверов Minecraft. Мы отказались от монолитных устаревших систем в пользу архитектуры, ориентированной на гибкость, скорость и полную интеграцию с современными ядрами (включая Folia).
🚀 Почему выбирают VaultMT?

    Исключительная производительность: Минимальное влияние на TPS сервера. Оптимизировано для работы на актуальных версиях Java.

    Архитектура Bridge: Уникальная система провайдеров позволяет работать как в автономном режиме, так и интегрироваться с существующими системами (например, EssentialsX).

    Полная поддержка Folia: Native-поддержка многопоточной среды.

    Гибкий конфиг: Полный контроль над экономикой — от символа валюты до интервалов автосохранения в config.yml.

    Developer-Friendly: Чистое и документированное API для ваших собственных плагинов и ботов.

⌨️ Команды
    
    /emt balance [игрок] — Узнать баланс
    /emt pay <игрок> <сумма> — Перевести деньги игроку
    /emt stats — Показать экономическую статистику сервера
    /emt give <игрок> <сумма> — Выдать монеты игроку
    /emt take <игрок> <сумма> — Забрать монеты у игрока
    /emt set <игрок> <сумма> — Установить точный баланс
    /emt log <игрок> — Посмотреть историю транзакций
    /emt version — Показать версию плагина
    /emt reload — Перезагрузить конфигурацию плагина

🧩 Placeholders (PlaceholderAPI)

    %vaultmt_balance% — текущий баланс игрока.
    %vaultmt_formatted% — баланс с символом валюты (из конфига)
    %vaultmt_fee% — текущая комиссия в процентах

📦 Установка

    Скачайте последнюю версию .jar файла.

    Поместите файл в папку ~/plugins вашего сервера.

    Запустите сервер для генерации config.yml.

    Настройте провайдеры и параметры под свои нужды.

🤝 Поддержка и разработка

VaultMT — это открытый проект. Мы приветствуем предложения по улучшению и сообщения об ошибках (issues). Если вы разработчик и хотите интегрировать VaultMT в свой проект, ознакомьтесь с нашим Wiki/API-гайдом.