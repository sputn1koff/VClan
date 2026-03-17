# Что было изменено в плагине?

## 1. Добавлена поддержка кастомных голов Base64. Использование: 
```
material: 'eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZjVlNDE3NDIzNmJmYWExYmUxMzVjMGI5OGE4ODBkM2ZmNjYzMzk0MzNjYzBkMDI4MTM4YmQ1OWFjZjdjOWQ5YiJ9fX0='
```

<img width="370" height="234" alt="изображение" src="https://github.com/user-attachments/assets/9925194b-8ec8-4fb8-86aa-8efc02ca5afe" />


## 2. Добавлена анимация при открытии меню. Использование (glow-menu.yml, main-menu.yml, player-list-menu.yml, settings-menu.yml, shop-menu.yml): 
```
  Animation:
    Enabled: true
    Type: "RANDOM" # CENTER, RIGHT, LEFT, TOP_DOWN, BOTTOM_UP, RANDOM
    DelayTicks: 2
    FillerMaterial: "AIR"
    FillerName: ""
    Sound:
      Enabled: true
      Name: "BLOCK_METAL_PLACE"
      Volume: 0.5
      Pitch: 1.5
```
// Если у вас не появилась анимация, то проверьте конфиг меню и наличие секции "Animation". Если у вас нет этой секции, то скопируйте и вставьте (идёт перед настройкой предметов, items: ...). 

![03172-ezgif com-video-to-gif-converter](https://github.com/user-attachments/assets/cf69fa3a-edac-45ec-b640-a1a399eb0062)

