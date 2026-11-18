# BloodDesignerAdvance

Аддон [AFriends](https://github.com/NoNames-Org/AFriends) для серверов `bloodstone.boo` и `extra.bloodstone.boo`.

- **Пати** — кнопка «Пати» в панели AFriends у чата: приглашение (только игрокам в сети), кик, роспуск, выход,
  принятие приглашений; «Позвать в пати» в меню друга и переписки; карточка приглашения в ЛС.
- **Батл пасс и задания** — своё меню вместо серверного.
- **Магазин** — своё меню `/donate` с подтверждением покупки.
- **Выбор режима** — кнопка в меню паузы: карточки режимов, поиск, избранное.

Каждую часть можно выключить: настройки AFriends → «Bloodstone». Настройки и избранные режимы при первом запуске
переносятся из конфига BloodDesigner.

## Установка

Нужны AFriends 2.0.0+, Fabric Loader 0.20.1+ и Fabric API. Файл берётся под ту же версию Minecraft, что и AFriends.

## Сборка

Нужен JDK 25. Сначала опубликовать AFriends локально, потом собрать аддон:

```
cd ../BloodDesigner && ./gradlew publishToMavenLocal --no-parallel
cd ../BloodDesignerAdvance && ./gradlew build dist
```

## Лицензия

MIT.
