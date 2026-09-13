<div align="center">

# HeaveVisuals

**Визуальный клиент-мод для Minecraft 1.21.11 (Fabric)**

[![Minecraft 1.21.11](https://img.shields.io/badge/Minecraft-1.21.11-brightgreen.svg?style=flat-square)](https://minecraft.net/)
[![Fabric](https://img.shields.io/badge/Loader-Fabric-blue.svg?style=flat-square)](https://fabricmc.net/)
[![Java 21](https://img.shields.io/badge/Java-21-orange.svg?style=flat-square)](https://adoptium.net/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg?style=flat-square)](LICENSE)

</div>

---

## 🌟 Особенности (Features)

<details>
<summary><b>👁️ Визуальные модули (Visuals)</b></summary>

* **NameTags** — чистые никнеймы без темного фона и теней, с защитой от просвечивания невидимых игроков сквозь стены.
* **Hitboxes** — настраиваемые хитбоксы, корректное отображение невидимых игроков в броне.
* **HitWaves & HitParticles** — визуальные эффекты и волны при нанесении урона.
* **Target ESP & China Hat** — подсветка целей и кастомные головные уборы.
* **ShaderHands & BlockOverlay** — шейдерные руки и подсветка блоков.
* **FullBright & TimeChanger** — постоянное освещение и управление временем суток.
</details>

<details>
<summary><b>📊 Интерфейс и HUD (Interface)</b></summary>

* **UKU Armor HUD** — статичный HUD брони в стиле UKU с привязкой к хотбару (справа или слева).
* **TargetHUD & Watermark** — отображение информации о цели и стильный вотермарк.
* **Potions & Cooldowns** — таймеры активных эффектов зелий и кулдаунов способностей.
* **ClickGUI** — кастомизируемое меню со сглаженной анимацией и звуками.
</details>

<details>
<summary><b>⚡ Утилиты и оптимизация (Utils)</b></summary>

* **Бесшумные бинды** — отключение звуков при переключении модулей горячими клавишами.
* **ItemScroller & HandSwap** — быстрая прокрутка инвентаря и свап предметов.
* **StreamerMode** — защита от показа личных данных на трансляциях.
* **Оптимизация** — оптимизация рендера и частиц для повышенного FPS.
</details>

---

## 🚀 Установка (Installation)

1. Установите **Minecraft 1.21.11** с **Fabric Loader**.
2. Скачайте актуальный файл `.jar` из раздела **Releases**.
3. Поместите `.jar` файл в папку `.minecraft/mods` или в профиль Lunar Client:
   ```text
   .lunarclient/profiles/1.21/mods/fabric-1.21.11/
   ```

---

## 🔨 Сборка из исходников (Building)

Для сборки требуется **Java 21 (JDK 21)**.

```bash
# Windows
.\gradlew.bat build -x test

# Linux / macOS
./gradlew build -x test
```

Готовый файл мода будет находиться в папке `build/libs/`.

---

## 📜 Лицензия (License)

Проект распространяется под открытой лицензией [MIT](LICENSE).

---


