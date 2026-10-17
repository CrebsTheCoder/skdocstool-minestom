# Minestom skDocs Tool

A documentation tool for skdocs for [Skript-Minestom](https://github.com/skript-minestom/skript-minestom) 

## Requirements and build

- Java 25
- Skript-Minestom 1.0.0-alpha.48 (Minecraft 26.2)

## Install and use

Place the tool and addon jars inside `Skript/addons/` beside your Skript-Minestom server jar, then restart the server.

| Command | Permission | Description |
| --- | --- | --- |
| `/gendocs` | `skdocstool.gendocs` | Generate documentation for Skript-Minestom and registered addons. |
| `/docs preview <addon>` | `skdocstool.docspreview` | Upload a generated file to the skDocs preview service and return its link. Supports addon name suggestions. |


Files are saved to `skdocstool/documentation/skdocs-<addon>.json` beside the server jar. Output is exclusively skDocs format. Generation stays local; the preview command uploads the chosen file when explicitly invoked.
## License

See [LICENSE](LICENSE).
