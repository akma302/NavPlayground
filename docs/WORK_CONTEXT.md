# Контекст работы — 2026-10-20

Снимок для продолжения в другой сессии. Перед действиями сверить ветку, status и log с Git.

## Цель и решения

Сравнить manual DI, Hilt, Koin и Metro в одном Android-приложении с Navigation 3, сохранив поведение.
Выбраны отдельные Git-ветки вместо копий проекта. Переключение DI в одной сборке не создаём.
Внедрены Metro, затем Koin с Classic DSL без compiler plugin, затем manual DI в main
по явному запросу пользователя. Сначала документация синхронизирована во всех ветках,
после этого main переведён с Hilt на manual DI.

Архитектура: [атлас DI](di/README.md), [Manual](di/manual.md), [Hilt](di/hilt.md), [Metro](di/metro.md), [Koin](di/koin.md).
Обзор и команды: [README](../README.md). Правила: [AGENTS](../AGENTS.md).

## Репозиторий и состояние

- Origin: https://github.com/akma302/NavPlayground.git
- `main`: manual DI, AppContainer + ManualViewModelFactory; без DI-фреймворков и KSP.
  Коммит реализации manual DI — `21d9b76`.
- `di/hilt`: исходная Hilt-реализация от `a028ebe`; обновлена только документация.
- `di/metro`: Metro 1.4.5, миграция `3e140eb`; схемы и документация — `f96f7a9`.
- `di/koin`: Koin 4.2.2 внедрён от исходного Hilt; Hilt/KSP удалены.
  Коммит миграции Koin — `aecd89c`.
- Общие документы синхронизируются во всех четырёх ветках отдельно от кода.
  Корневой README в каждой ветке описывает её собственную реализацию.
- Все четыре схемы отражают реализованный код. Manual-схема ссылается на `21d9b76`. Ссылки на Hilt и Metro закреплены
  за коммитами; Koin — `aecd89c`. Подробности не дублируются в контексте.

## Проверки

Для manual, Koin и Metro выполнено:

```powershell
.\gradlew.bat clean :app:assembleDebug testDebugUnitTest --console=plain
```

- Manual: BUILD SUCCESSFUL, 93 теста, 0 failures/errors.
- Metro: BUILD SUCCESSFUL, 91 тест, 0 failures/errors.
- Koin: BUILD SUCCESSFUL, 93 теста, 0 failures/errors.
- Для всех трёх создан debug APK: `app/build/outputs/apk/debug/app-debug.apk`.
  Этот локальный путь содержит артефакт последней собранной ветки, не все версии одновременно.
- KoinGraphTest поднимает production appModules с `allowOverride(false)` и проверяет
  identity общих объектов, 5 entries / 7 middleware / 3 handlers / 5 serializer contributions,
  Koin-фабрику четырёх ViewModel, разделение/очистку ViewModelStore,
  auth/feature flags, отмену coroutine scope при закрытии и свежие singles нового контейнера.
- ManualGraphTest проверяет production AppContainer, все коллекции/сериализаторы,
  фабрику четырёх ViewModel, ViewModelStore, общие объекты, auth/flags, cleanup и неизвестную ViewModel.
- MetroGraphTest проверяет сгенерированный проверочный граф с production-вкладами.

Документация: пять Mermaid-диаграмм проверены парсером Mermaid 11.12.0;
локальные Markdown-ссылки проверены во всех четырёх ветках.

## Что остаётся

- UI на устройстве/эмуляторе и release-сборки не проверялись.
- Следующая полезная проверка всех реализаций: auth redirect, media result, deep links,
  поворот и восстановление после смерти процесса.
- Koin использует runtime resolution без compile-time проверки: текущие регистрации покрыты
  тестами, но новые definitions требуют актуализации тестов. Compiler plugin можно оценить отдельно.
- Общие исправления логики переносить между ветками отдельно от DI-изменений.
- Ожидания результатов навигации живут в памяти и не переживают смерть процесса:
  это существующее ограничение.

## Среда

Windows / PowerShell; Gradle 9.6.0, AGP 9.4.0, Kotlin 2.4.21.
Daemon JVM 25 задан в `gradle/gradle-daemon-jvm.properties`; SDK — в игнорируемом local.properties.
Gradle-кеш/сеть и Git push могут требовать разрешение. `gh` не установлен;
обычный Git push работает через credential helper.
