[![Buy Me A Coffee](https://img.shields.io/badge/Buy%20Me%20a%20Coffee-ffdd00?style=for-the-badge&logo=buy-me-a-coffee&logoColor=black)](https://www.youtube.com/watch?v=xvFZjo5PgG0)

# Anvil Enchantment Ordering Guide

![Mod Icon](common/src/main/resources/assets/aeog/icon.png)

## Installation

* [Modrinth](https://modrinth.com/mod/anvil-enchantment-ordering-guide)
* [CurseForge](https://www.curseforge.com/minecraft/mc-mods/anvil-enchantment-ordering-guide)

## Acknowledgements
   
The enchantment merge optimization logic used in this mod is based on the work found at https://github.com/iamcal/enchant-order. A sincere thank you to the author [iamcal](https://github.com/iamcal) for allowing the use of their logic in this project.

## Support
   
If you encounter bugs or wish to contribute:
* [Report any problems you find.](https://github.com/armaninyow/Anvil-Enchantment-Ordering-Guide/discussions/categories/issues)
* [Share your ideas for new features.](https://github.com/armaninyow/Anvil-Enchantment-Ordering-Guide/discussions/categories/suggestions)

## Changelog
<details>
  <summary></summary>
   
### 2.0.0—1.21.x
* Added multi-version support covering Minecraft 1.21 through 1.21.11
* Replaced the custom click sound with a built-in Minecraft sound
* Moved Carved Pumpkin to the end of the item grid in Phase 1, next to Book
* Redesigned Phase 2 enchantment rows to occupy a single row each instead of two
* Made enchantment slots clickable to reveal a centered level picker inline
* Added level 0 to the level picker as a way to deselect an enchantment
### 1.2.0—1.21.11
* Made the mod fully client-side. No longer needs to be installed on the server
* Added a List View option for Phase 3 in the mod settings, showing merge steps as a scrollable numbered list instead of the draggable tree
* Fixed the game freezing before the loading screen appeared when clicking Calculate
* Fixed the result not showing after calculation until the guide button was toggled off and on again
### 1.1.0—1.21.11
* Added Auto-detect Item setting: automatically advances to Phase 2 with the correct item pre-selected when an item is placed in the anvil's left slot
* Added Auto-fill Mode setting: optionally pre-selects enchantment levels in Phase 2, either at maximum level for all applicable enchantments or based on enchanted books found in the player's inventory
* Added Allow Incompatible Enchantments setting: allows selecting mutually exclusive enchantments simultaneously, giving each its own independent row instead of grouping them with prev/next arrows
* Added a loading screen for calculations with 10 or more enchantments. Phase 3 opens immediately on clicking Calculate, showing a progress bar and the message "Calculating... Please keep the anvil open." The bar fills based on an estimated duration calibrated from real timing data, capped at 90% until the result arrives
* Reduced leaf node spacing so frames are packed with exactly 1px between each edge, removing the visual gap between books in the top row
* Implemented persistent panel state so the tree, selected item, enchantment choices, and scroll position are all restored when the anvil screen is closed and reopened
* Implemented automatic panel reopening when the anvil screen reinitialises, with the centering shift correctly reapplied
* Changed curse enchantment names and their selected level numbers to render in red in both Phase 2 and the Phase 3 tooltip, distinguishing them from regular enchantments
* Fixed the merge tree layout to correctly handle all enchantment combinations, including cases where the optimizer builds the full book tree first and merges the item in near the end
* Fixed the base item (sword, boots, axe, etc.) always appearing as the leftmost node in the top row regardless of enchantment count or settings
* Fixed intermediate merge results that feed into the same next step always appearing on the same row, eliminating unnecessarily long vertical connector lines
* Fixed connector lines crossing over each other by ensuring each pair's books are always placed adjacent in the top row
* Fixed result nodes overlapping at the bottom of the tree as a consequence of the connector crossing fix
### 1.0.0—1.21.11
* Added Spear to Phase 1 item selection, placed between Mace and Trident
* Added Lunge enchantment (spear-exclusive, max level III)
### 1.0.0—1.21.10
* Initial Release
</details>

[![Buy Me A Coffee](https://img.shields.io/badge/Buy%20Me%20a%20Coffee-ffdd00?style=for-the-badge&logo=buy-me-a-coffee&logoColor=black)](https://www.youtube.com/watch?v=xvFZjo5PgG0)
