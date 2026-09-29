from pathlib import Path
from docx import Document
from docx.enum.section import WD_SECTION
from docx.enum.table import WD_CELL_VERTICAL_ALIGNMENT, WD_ROW_HEIGHT_RULE, WD_TABLE_ALIGNMENT
from docx.enum.text import WD_ALIGN_PARAGRAPH, WD_BREAK
from docx.oxml import OxmlElement
from docx.oxml.ns import qn
from docx.shared import Inches, Pt, RGBColor


ROOT = Path(__file__).resolve().parents[2]
OUTPUT = ROOT / "docs" / "Finni-technical-documentation.docx"

NAVY = "17324D"
PURPLE = "6D45B8"
ORANGE = "F27935"
MINT = "DDF4EC"
PALE_PURPLE = "EEE8FA"
PALE_ORANGE = "FFF0E7"
PALE_BLUE = "EAF3F8"
LIGHT = "F5F7F9"
MID = "D6DEE5"
INK = "1E2933"
WHITE = "FFFFFF"
GREEN = "247A62"
RED = "A63C31"


def set_cell_shading(cell, fill):
    tc_pr = cell._tc.get_or_add_tcPr()
    shd = tc_pr.find(qn("w:shd"))
    if shd is None:
        shd = OxmlElement("w:shd")
        tc_pr.append(shd)
    shd.set(qn("w:fill"), fill)


def set_cell_margins(cell, top=90, start=100, bottom=90, end=100):
    tc = cell._tc
    tc_pr = tc.get_or_add_tcPr()
    tc_mar = tc_pr.first_child_found_in("w:tcMar")
    if tc_mar is None:
        tc_mar = OxmlElement("w:tcMar")
        tc_pr.append(tc_mar)
    for margin, value in (("top", top), ("start", start), ("bottom", bottom), ("end", end)):
        node = tc_mar.find(qn(f"w:{margin}"))
        if node is None:
            node = OxmlElement(f"w:{margin}")
            tc_mar.append(node)
        node.set(qn("w:w"), str(value))
        node.set(qn("w:type"), "dxa")


def set_cell_width(cell, inches):
    tc_pr = cell._tc.get_or_add_tcPr()
    tc_w = tc_pr.find(qn("w:tcW"))
    if tc_w is None:
        tc_w = OxmlElement("w:tcW")
        tc_pr.append(tc_w)
    tc_w.set(qn("w:w"), str(int(inches * 1440)))
    tc_w.set(qn("w:type"), "dxa")


def set_repeat_table_header(row):
    tr_pr = row._tr.get_or_add_trPr()
    tbl_header = OxmlElement("w:tblHeader")
    tbl_header.set(qn("w:val"), "true")
    tr_pr.append(tbl_header)


def prevent_row_split(row):
    tr_pr = row._tr.get_or_add_trPr()
    cant_split = OxmlElement("w:cantSplit")
    tr_pr.append(cant_split)


def set_table_borders(table, color=MID, size=6):
    tbl_pr = table._tbl.tblPr
    borders = tbl_pr.find(qn("w:tblBorders"))
    if borders is None:
        borders = OxmlElement("w:tblBorders")
        tbl_pr.append(borders)
    for edge in ("top", "left", "bottom", "right", "insideH", "insideV"):
        tag = borders.find(qn(f"w:{edge}"))
        if tag is None:
            tag = OxmlElement(f"w:{edge}")
            borders.append(tag)
        tag.set(qn("w:val"), "single")
        tag.set(qn("w:sz"), str(size))
        tag.set(qn("w:space"), "0")
        tag.set(qn("w:color"), color)


def set_run_font(run, name="Arial", size=None, bold=None, color=None):
    run.font.name = name
    run._element.rPr.rFonts.set(qn("w:eastAsia"), name)
    if size is not None:
        run.font.size = Pt(size)
    if bold is not None:
        run.bold = bold
    if color is not None:
        run.font.color.rgb = RGBColor.from_string(color)


def add_page_number(paragraph):
    paragraph.alignment = WD_ALIGN_PARAGRAPH.RIGHT
    run = paragraph.add_run("Страница ")
    set_run_font(run, size=8, color="66717C")
    fld_char1 = OxmlElement("w:fldChar")
    fld_char1.set(qn("w:fldCharType"), "begin")
    instr_text = OxmlElement("w:instrText")
    instr_text.set(qn("xml:space"), "preserve")
    instr_text.text = "PAGE"
    fld_char2 = OxmlElement("w:fldChar")
    fld_char2.set(qn("w:fldCharType"), "end")
    run._r.extend([fld_char1, instr_text, fld_char2])


def setup_section(section):
    section.page_width = Inches(8.5)
    section.page_height = Inches(11)
    section.top_margin = Inches(0.62)
    section.bottom_margin = Inches(0.58)
    section.left_margin = Inches(0.62)
    section.right_margin = Inches(0.62)
    section.header_distance = Inches(0.28)
    section.footer_distance = Inches(0.28)
    header = section.header.paragraphs[0]
    header.text = "FINNI  |  ТЕХНИЧЕСКАЯ ДОКУМЕНТАЦИЯ"
    set_run_font(header.runs[0], size=8, bold=True, color=PURPLE)
    footer = section.footer.paragraphs[0]
    add_page_number(footer)


def configure_styles(doc):
    normal = doc.styles["Normal"]
    normal.font.name = "Arial"
    normal._element.rPr.rFonts.set(qn("w:eastAsia"), "Arial")
    normal.font.size = Pt(9.2)
    normal.font.color.rgb = RGBColor.from_string(INK)
    normal.paragraph_format.space_after = Pt(4)
    normal.paragraph_format.line_spacing = 1.08

    for name, size, color, before, after in (
        ("Title", 28, NAVY, 0, 12),
        ("Subtitle", 13, PURPLE, 0, 10),
        ("Heading 1", 19, NAVY, 10, 6),
        ("Heading 2", 13.5, PURPLE, 8, 4),
        ("Heading 3", 10.5, ORANGE, 6, 3),
    ):
        style = doc.styles[name]
        style.font.name = "Arial"
        style._element.rPr.rFonts.set(qn("w:eastAsia"), "Arial")
        style.font.size = Pt(size)
        style.font.bold = True
        style.font.color.rgb = RGBColor.from_string(color)
        style.paragraph_format.space_before = Pt(before)
        style.paragraph_format.space_after = Pt(after)
        style.paragraph_format.keep_with_next = True


def paragraph(doc, text="", style=None, bold_prefix=None, color=None, align=None):
    p = doc.add_paragraph(style=style)
    if align is not None:
        p.alignment = align
    if bold_prefix and text.startswith(bold_prefix):
        r1 = p.add_run(bold_prefix)
        set_run_font(r1, bold=True, color=color)
        r2 = p.add_run(text[len(bold_prefix):])
        set_run_font(r2, color=color)
    else:
        run = p.add_run(text)
        set_run_font(run, color=color)
    return p


def bullet(doc, text, level=0):
    p = doc.add_paragraph(style="List Bullet" if level == 0 else "List Bullet 2")
    p.paragraph_format.space_after = Pt(2)
    run = p.add_run(text)
    set_run_font(run, size=9.2)
    return p


_number_counter = 0


def reset_numbers():
    global _number_counter
    _number_counter = 0


def number(doc, text):
    global _number_counter
    _number_counter += 1
    p = doc.add_paragraph()
    p.paragraph_format.space_after = Pt(2)
    p.paragraph_format.left_indent = Inches(0.22)
    p.paragraph_format.first_line_indent = Inches(-0.22)
    set_run_font(p.add_run(f"{_number_counter}."), size=9.2, bold=True, color=NAVY)
    set_run_font(p.add_run(f"  {text}"), size=9.2)
    return p


def callout(doc, title, text, fill=PALE_PURPLE, accent=PURPLE):
    table = doc.add_table(rows=1, cols=2)
    table.alignment = WD_TABLE_ALIGNMENT.CENTER
    table.autofit = False
    set_cell_width(table.cell(0, 0), 0.12)
    set_cell_width(table.cell(0, 1), 7.0)
    set_cell_shading(table.cell(0, 0), accent)
    set_cell_shading(table.cell(0, 1), fill)
    set_cell_margins(table.cell(0, 0), 40, 20, 40, 20)
    set_cell_margins(table.cell(0, 1), 130, 150, 130, 150)
    cell = table.cell(0, 1)
    p = cell.paragraphs[0]
    p.paragraph_format.space_after = Pt(3)
    set_run_font(p.add_run(title), bold=True, size=10, color=accent)
    p2 = cell.add_paragraph()
    p2.paragraph_format.space_after = Pt(0)
    set_run_font(p2.add_run(text), size=9.2, color=INK)
    doc.add_paragraph().paragraph_format.space_after = Pt(0)


def code_block(doc, lines):
    table = doc.add_table(rows=1, cols=1)
    table.alignment = WD_TABLE_ALIGNMENT.CENTER
    cell = table.cell(0, 0)
    set_cell_shading(cell, "F0F3F5")
    set_cell_margins(cell, 110, 140, 110, 140)
    p = cell.paragraphs[0]
    p.paragraph_format.space_after = Pt(0)
    p.paragraph_format.line_spacing = 1.0
    for index, line in enumerate(lines):
        if index:
            p.add_run().add_break()
        run = p.add_run(line)
        set_run_font(run, name="Courier New", size=8.2, color="26333D")
    doc.add_paragraph().paragraph_format.space_after = Pt(0)


def add_table(doc, headers, rows, widths=None, font_size=8.1, header_fill=NAVY):
    table = doc.add_table(rows=1, cols=len(headers))
    table.alignment = WD_TABLE_ALIGNMENT.CENTER
    table.autofit = False
    set_table_borders(table)
    header = table.rows[0]
    set_repeat_table_header(header)
    prevent_row_split(header)
    for idx, text in enumerate(headers):
        cell = header.cells[idx]
        set_cell_shading(cell, header_fill)
        set_cell_margins(cell)
        cell.vertical_alignment = WD_CELL_VERTICAL_ALIGNMENT.CENTER
        p = cell.paragraphs[0]
        p.paragraph_format.space_after = Pt(0)
        set_run_font(p.add_run(str(text)), size=font_size, bold=True, color=WHITE)
        if widths:
            set_cell_width(cell, widths[idx])
    for row_index, values in enumerate(rows):
        row = table.add_row()
        prevent_row_split(row)
        if row_index % 2 == 1:
            for cell in row.cells:
                set_cell_shading(cell, "FAFBFC")
        for idx, value in enumerate(values):
            cell = row.cells[idx]
            set_cell_margins(cell)
            cell.vertical_alignment = WD_CELL_VERTICAL_ALIGNMENT.TOP
            if widths:
                set_cell_width(cell, widths[idx])
            p = cell.paragraphs[0]
            p.paragraph_format.space_after = Pt(0)
            p.paragraph_format.line_spacing = 1.0
            text = str(value)
            color = INK
            bold = False
            if text.startswith("Реализовано"):
                color = GREEN
                bold = True
            elif text.startswith("Частично") or text.startswith("Не завершено"):
                color = RED
                bold = True
            set_run_font(p.add_run(text), size=font_size, bold=bold, color=color)
    doc.add_paragraph().paragraph_format.space_after = Pt(0)
    return table


def section_title(doc, number_text, title, subtitle=None):
    p = doc.add_paragraph(style="Heading 1")
    run = p.add_run(f"{number_text}  {title}")
    set_run_font(run, size=19, bold=True, color=NAVY)
    if subtitle:
        p2 = doc.add_paragraph()
        p2.paragraph_format.space_after = Pt(6)
        set_run_font(p2.add_run(subtitle), size=9.5, color="5D6872")


def add_page_break(doc):
    doc.add_paragraph().add_run().add_break(WD_BREAK.PAGE)


def flow_table(doc, steps):
    table = doc.add_table(rows=1, cols=len(steps) * 2 - 1)
    table.alignment = WD_TABLE_ALIGNMENT.CENTER
    table.autofit = False
    for i, step in enumerate(steps):
        cell = table.cell(0, i * 2)
        set_cell_width(cell, 1.24)
        set_cell_shading(cell, PALE_PURPLE if i % 2 == 0 else PALE_BLUE)
        set_cell_margins(cell, 110, 60, 110, 60)
        p = cell.paragraphs[0]
        p.alignment = WD_ALIGN_PARAGRAPH.CENTER
        set_run_font(p.add_run(step), size=8.1, bold=True, color=NAVY)
        if i < len(steps) - 1:
            arrow = table.cell(0, i * 2 + 1)
            set_cell_width(arrow, 0.25)
            p2 = arrow.paragraphs[0]
            p2.alignment = WD_ALIGN_PARAGRAPH.CENTER
            set_run_font(p2.add_run("→"), size=14, bold=True, color=ORANGE)
    return table


doc = Document()
configure_styles(doc)
for section in doc.sections:
    setup_section(section)

# Cover
p = doc.add_paragraph()
p.paragraph_format.space_before = Pt(64)
p.paragraph_format.space_after = Pt(8)
r = p.add_run("FINNI")
set_run_font(r, size=15, bold=True, color=ORANGE)

p = doc.add_paragraph(style="Title")
p.alignment = WD_ALIGN_PARAGRAPH.LEFT
set_run_font(p.add_run("Техническая документация"), size=28, bold=True, color=NAVY)

p = doc.add_paragraph(style="Subtitle")
set_run_font(p.add_run("Мобильное приложение «Финни Три кармашка»"), size=13, bold=True, color=PURPLE)

paragraph(
    doc,
    "Функциональный офлайн-прототип для формирования базовых финансовых навыков у детей 7–11 лет",
    color="45535E",
)

doc.add_paragraph().paragraph_format.space_after = Pt(14)
cover = doc.add_table(rows=3, cols=2)
cover.alignment = WD_TABLE_ALIGNMENT.LEFT
cover.autofit = False
cover_data = [
    ("Версия документа", "1.0"),
    ("Дата", "29 сентября 2026"),
    ("Состояние", "Документация текущего прототипа"),
]
for i, (key, value) in enumerate(cover_data):
    for c in cover.rows[i].cells:
        set_cell_margins(c, 120, 140, 120, 140)
    set_cell_width(cover.cell(i, 0), 1.75)
    set_cell_width(cover.cell(i, 1), 4.8)
    set_cell_shading(cover.cell(i, 0), NAVY)
    set_cell_shading(cover.cell(i, 1), LIGHT)
    set_run_font(cover.cell(i, 0).paragraphs[0].add_run(key), size=9, bold=True, color=WHITE)
    set_run_font(cover.cell(i, 1).paragraphs[0].add_run(value), size=9, color=INK)
set_table_borders(cover, color=WHITE, size=8)

doc.add_paragraph().paragraph_format.space_after = Pt(18)
callout(
    doc,
    "Честный статус готовности",
    "Основной сквозной сценарий и требуемый каталог из восьми товаров реализованы. Для полного формального соответствия финальной сдаче остается выпустить подписанный release APK и провести чистый сквозной прогон.",
    fill=PALE_ORANGE,
    accent=ORANGE,
)

p = doc.add_paragraph()
p.paragraph_format.space_before = Pt(52)
set_run_font(p.add_run("Репозиторий: MoscowCityHack26"), size=9, bold=True, color=NAVY)
p2 = doc.add_paragraph()
set_run_font(p2.add_run("Пакет Android: com.mikhailskiy.finni"), size=9, color="5D6872")

add_page_break(doc)

# Document map
section_title(doc, "0", "Навигация по документу")
add_table(
    doc,
    ["Раздел", "Содержание"],
    [
        ("1", "Назначение, аудитория, границы продукта"),
        ("2", "Репозиторий, окружение, быстрый запуск и release APK"),
        ("3", "Функциональная и компонентная архитектура"),
        ("4", "Структура данных профиля, экономики, заданий и прогресса"),
        ("5", "Правила баланса, состояния, шагов и роста"),
        ("6", "Матрица соответствия обязательным требованиям"),
        ("7", "Карта образовательного контента"),
        ("8", "UX UI и доступность"),
        ("9", "Разрешения Android, данные и удаление профиля"),
        ("10", "Тест-кейсы и физическое устройство"),
        ("11", "Ограничения и развитие"),
        ("12", "Сторонние компоненты и лицензии"),
        ("Приложение", "Демонстрационный сценарий и ссылки на исходники"),
    ],
    widths=[0.95, 6.25],
    font_size=8.7,
)

doc.add_paragraph(style="Heading 2").add_run("Паспорт решения")
add_table(
    doc,
    ["Параметр", "Значение"],
    [
        ("Название", "Финни Три кармашка"),
        ("Платформа", "Android 8.0+, portrait-first"),
        ("Целевая аудитория", "Дети 7–11 лет; взрослый — поддерживающий пользователь"),
        ("Режим", "Офлайн, один локальный профиль, отдельный демопрофиль"),
        ("Технологии", "Kotlin, Jetpack Compose, Material 3, StateFlow, SQLiteOpenHelper"),
        ("Сервер", "Отсутствует; OpenAPI и серверное развертывание не применимы"),
        ("Деньги", "Только условная игровая валюта без реальной стоимости"),
        ("Текущая версия", "versionName 1.0, versionCode 1"),
    ],
    widths=[1.55, 5.65],
    font_size=8.6,
)

add_page_break(doc)

# 1 Product
section_title(doc, "1", "Назначение продукта")
paragraph(doc, "Финни превращает последовательность финансовых решений в понятный ребенку результат. Пользователь сначала планирует, затем действует и сразу видит, как изменились баланс, накопления, цель и робот-питомец.")

doc.add_paragraph(style="Heading 2").add_run("Образовательные цели")
for text in (
    "Понимать, что расходы не должны превышать доступный доход.",
    "Различать обязательное и желаемое.",
    "Планировать простые покупки в условиях ограниченного бюджета.",
    "Регулярно откладывать на краткосрочную цель.",
    "Сравнивать план с фактом и объяснять последствия решений.",
):
    bullet(doc, text)

doc.add_paragraph(style="Heading 2").add_run("Ключевые возможности")
for text in (
    "Гостевой профиль и создание персонажа без регистрации.",
    "Бюджет из трех направлений: Забота, Радость, Мечта.",
    "8 товаров, 4 цели, 6 заданий и неограниченные игровые периоды.",
    "Журнал каждой операции с источником, суммой и объяснением.",
    "Три стадии развития и обратимые показатели состояния.",
    "Фоновый шагомер: физическая активность расходует сытость.",
    "Раздел взрослого, сброс демопрофиля и локальное удаление данных.",
):
    bullet(doc, text)

doc.add_paragraph(style="Heading 2").add_run("Границы")
callout(
    doc,
    "Безопасный просветительский прототип",
    "Нет банковских подключений, реальных платежей, рекламы, подписок, социальных функций, облака или персональных рекомендаций. Ошибка не отнимает достигнутый рост и всегда допускает восстановление через следующий выбор.",
    fill=PALE_BLUE,
    accent=GREEN,
)

flow_table(doc, ["План", "Выбор", "Эффект", "Объяснение", "Рост"])

add_page_break(doc)

# 2 Build
section_title(doc, "2", "Репозиторий и сборка")
doc.add_paragraph(style="Heading 2").add_run("Состав репозитория")
code_block(doc, [
    "app/src/main/java/.../data/     SQLite, DAO, репозиторий, настройки, шагомер",
    "app/src/main/java/.../domain/   модели, каталог, экономика и рост",
    "app/src/main/java/.../ui/       ViewModel, навигация, Compose-экраны",
    "app/src/main/res/               изображения, темы, XML-конфигурация",
    "app/src/test/                   unit-тесты доменных правил",
    "docs/                           концепция, архитектура, ТЗ изображений, DOCX",
    "gradle/                         version catalog и Gradle Wrapper",
])

doc.add_paragraph(style="Heading 2").add_run("Версии окружения")
add_table(
    doc,
    ["Компонент", "Версия или требование"],
    [
        ("Android Studio", "версия с поддержкой AGP 9.3.x"),
        ("JDK", "17 рекомендуется для Gradle; Java 11 target"),
        ("Android Gradle Plugin", "9.3.2"),
        ("Gradle Wrapper", "9.5.0"),
        ("Kotlin", "2.2.10"),
        ("SDK", "compile 37, target 37, min 26"),
        ("Compose BOM", "2026.02.01"),
        ("JUnit", "4.13.2"),
    ],
    widths=[2.4, 4.8],
    font_size=8.7,
)

doc.add_paragraph(style="Heading 2").add_run("Быстрый запуск")
reset_numbers()
number(doc, "Открыть корень репозитория в Android Studio и дождаться Gradle Sync.")
number(doc, "Подключить устройство Android 8.0+ с USB-отладкой или эмулятор API 26+.")
number(doc, "Выбрать конфигурацию app и нажать Run.")
number(doc, "На устройстве со счетчиком шагов разрешить физическую активность по запросу.")
code_block(doc, [
    "./gradlew testDebugUnitTest lintDebug assembleDebug",
    "adb install -r app/build/outputs/apk/debug/app-debug.apk",
])

doc.add_paragraph(style="Heading 2").add_run("Release APK")
reset_numbers()
number(doc, "Установить JDK 17 и Android SDK 37; проверить путь sdk.dir в local.properties.")
number(doc, "Запустить ./gradlew clean testDebugUnitTest lintDebug assembleRelease.")
number(doc, "Получить app/build/outputs/apk/release/app-release-unsigned.apk.")
number(doc, "В Android Studio открыть Build → Generate Signed App Bundle or APK → APK.")
number(doc, "Выбрать модуль app, release и keystore, хранящийся вне репозитория.")
number(doc, "Проверить подпись apksigner и установить APK на чистое физическое устройство.")

callout(
    doc,
    "Важно",
    "Текущий Gradle-проект не содержит signingConfig, keystore или пароль. Поэтому assembleRelease создает неподписанный артефакт. Подписанный APK для финальной сдачи — отдельный незавершенный пункт.",
    fill=PALE_ORANGE,
    accent=ORANGE,
)

add_page_break(doc)

# 3 Architecture
section_title(doc, "3", "Архитектура")
doc.add_paragraph(style="Heading 2").add_run("Функциональный контур")
flow_table(doc, ["Профиль", "Период", "План", "Действия", "Итог"])
paragraph(doc, "Период начинается с дохода 100 монет. Подтвержденный план открывает покупки, задания и накопления. Все движения валюты проходят через единый журнал. Завершение периода сравнивает план с фактом и начисляет очки роста.")

doc.add_paragraph(style="Heading 2").add_run("Компонентный контур")
add_table(
    doc,
    ["Слой", "Ответственность", "Исходник"],
    [
        ("Compose UI", "Экраны, подтверждения, навигация и обратная связь", "ui/screens, ui/components, FinniApp.kt"),
        ("ViewModel", "StateFlow, busy/error state, вызовы операций", "ui/FinniViewModel.kt"),
        ("Domain", "Модели, каталог, формулы, стадии", "domain/GameModels.kt, GameCatalog.kt"),
        ("Repository", "Транзакционные use cases и GameSnapshot", "data/FinniRepository.kt"),
        ("DAO", "SQL-чтение и запись", "data/FinniDao.kt"),
        ("Database", "11 таблиц, ограничения, миграции v1–v5", "data/FinniDatabase.kt"),
        ("Step service", "Foreground service и дельта TYPE_STEP_COUNTER", "data/StepCounterService.kt"),
        ("Settings", "Звук и уменьшение анимации", "data/SettingsRepository.kt"),
    ],
    widths=[1.25, 3.25, 2.7],
    font_size=7.8,
)

doc.add_paragraph(style="Heading 2").add_run("Поток данных")
flow_table(doc, ["Экран", "ViewModel", "Repository", "DAO", "SQLite"])
paragraph(doc, "После каждой команды ViewModel перечитывает согласованный GameSnapshot. Финансовые операции, изменение состояния и фиксация истории выполняются внутри одной SQLite-транзакции.")

doc.add_paragraph(style="Heading 2").add_run("Серверная часть")
callout(
    doc,
    "Не применяется",
    "Сервер отсутствует. В Manifest нет INTERNET, сетевой клиент не подключен, данные не отправляются. Поэтому OpenAPI и схема серверного развертывания не требуются. Развертывание — сборка, подпись и установка APK.",
    fill=PALE_BLUE,
    accent=NAVY,
)

doc.add_paragraph(style="Heading 2").add_run("Навигация")
add_table(
    doc,
    ["Поток", "Экраны"],
    [
        ("Первый запуск", "Intro → CreateProfile → CreatePet"),
        ("Игровой цикл", "Home → Budget / Tasks / Shop / Savings → Summary"),
        ("Обучение", "TaskPlay, Help, Progress"),
        ("Взрослый", "AdultGate → Adult"),
    ],
    widths=[1.6, 5.6],
    font_size=8.8,
)

add_page_break(doc)

# 4 Data
section_title(doc, "4", "Структура данных")
paragraph(doc, "SQLite-схема версии 5 содержит 11 связанных таблиц. Внешние ключи включены, дочерние записи профиля удаляются каскадно. Денежные операции используют снимки значений, чтобы будущая правка каталога не меняла историю.")

add_table(
    doc,
    ["Таблица", "Ключевые поля", "Назначение"],
    [
        ("profiles", "profile_id, nickname, avatar_id, is_demo", "Один локальный профиль"),
        ("pets", "species, color, equipment, stage, growth, state", "Герой и текущее состояние"),
        ("game_periods", "number, template, content_version, status", "Игровые циклы"),
        ("budget_plans", "base, required, optional, savings, buffer", "План периода"),
        ("ledger_entries", "type, deltas, balances_after, source, explanation", "Источник истины для валюты"),
        ("purchases", "item snapshot, category, price, effects", "История покупок"),
        ("goal_progress", "goal snapshot, target, status, timestamps", "Финансовые цели"),
        ("task_attempts", "task, content_version, actions, reward, result", "Учебный прогресс"),
        ("pet_state_events", "reason, deltas, state_after, message", "Объяснимая история героя"),
        ("period_summaries", "actuals, scores, balances, growth, feedback", "Итог периода"),
        ("step_tracking", "last_total, tracked, pending", "Учет дельты шагов"),
    ],
    widths=[1.35, 3.05, 2.8],
    font_size=7.5,
)

doc.add_paragraph(style="Heading 2").add_run("Связи")
add_table(
    doc,
    ["Родитель", "Связь", "Дочерние данные"],
    [
        ("profiles", "1 : 0..1", "pets, step_tracking"),
        ("profiles", "1 : N", "game_periods, ledger_entries, purchases, goals, tasks"),
        ("game_periods", "1 : 1", "budget_plans, period_summaries после закрытия"),
        ("game_periods", "1 : N", "purchases, task_attempts, ledger_entries"),
        ("pets", "1 : N", "pet_state_events"),
    ],
    widths=[1.5, 1.0, 4.7],
    font_size=8.4,
)

doc.add_paragraph(style="Heading 2").add_run("Структуры предметной области")
for title, text in (
    ("Профиль", "ProfileEntity, PetEntity и AppSettings."),
    ("Игровая экономика", "LedgerEntryEntity — источник истины; PurchaseEntity — снимок расхода."),
    ("Задания", "Определения GameCatalog.tasks; результаты TaskAttemptEntity."),
    ("Прогресс", "GoalProgressEntity, PeriodSummaryEntity, PetStateEventEntity и StepTrackingEntity."),
    ("UI-снимок", "GameSnapshot агрегирует согласованное состояние для экранов."),
):
    paragraph(doc, f"{title}. {text}", bold_prefix=f"{title}.")

add_page_break(doc)

# 5 Rules
section_title(doc, "5", "Игровые правила и формулы")
doc.add_paragraph(style="Heading 2").add_run("Баланс")
code_block(doc, [
    "available_after = available_before + available_delta",
    "savings_after   = savings_before   + savings_delta",
])
add_table(
    doc,
    ["Операция", "Доступно", "Накопления"],
    [
        ("Старт периода", "+100", "0"),
        ("Задание", "+10 или +15", "0"),
        ("Покупка", "−price", "0"),
        ("Взнос в цель", "−amount", "+amount"),
        ("Возврат", "+amount", "−amount"),
    ],
    widths=[3.2, 2.0, 2.0],
    font_size=8.7,
)

doc.add_paragraph(style="Heading 2").add_run("Бюджет")
code_block(doc, [
    "required ≥ 0; optional ≥ 0; savings ≥ 0",
    "required + optional + savings ≤ base_available",
    "buffer = base_available − required − optional − savings",
])

doc.add_paragraph(style="Heading 2").add_run("Состояние героя")
code_block(doc, ["state_after = clamp(state_before + item_delta, 0, 100)"])
paragraph(doc, "Покупки повышают сытость, настроение или уют. В новом периоде настроение и уют уменьшаются на 5, но не ниже 20. Рост не отнимается.")

doc.add_paragraph(style="Heading 2").add_run("Шаги и сытость")
code_block(doc, [
    "threshold = 500 + (shoes ? 500 : 0) + (jetpack ? 1000 : 0)",
    "accumulated = pending_steps + new_steps",
    "satiety_loss = floor(accumulated / threshold)",
    "pending_after = accumulated mod threshold",
    "satiety_after = max(0, satiety_before − satiety_loss)",
])
add_table(
    doc,
    ["Экипировка", "Шагов на −1 сытости"],
    [("Нет", "500"), ("Энергообувь", "1 000"), ("Рюкзак", "1 500"), ("Оба предмета", "2 000")],
    widths=[3.6, 3.6],
    font_size=8.8,
)

doc.add_paragraph(style="Heading 2").add_run("Рост")
code_block(doc, [
    "growth = essentials + follows_plan + saves_regularly",
    "essentials = 10, если куплен ≥1 обязательный товар, иначе 0",
    "follows_plan = 5, если оба факта расходов не превышают план, иначе 0",
    "saves_regularly = 5, если взносы ≥ плану накоплений, иначе 0",
])
add_table(
    doc,
    ["Стадия", "Очки"],
    [("Малыш", "0–29"), ("Исследователь", "30–69"), ("Знаток", "70+")],
    widths=[3.6, 3.6],
    font_size=8.8,
)

doc.add_paragraph(style="Heading 2").add_run("Оценка срока цели")
code_block(doc, [
    "remaining = max(0, target − savings)",
    "estimated_periods = ceil(remaining / max(planned_savings, 5))",
])

doc.add_paragraph(style="Heading 2").add_run("Пример расчета периода")
add_table(
    doc,
    ["Шаг", "Изменение", "Результат"],
    [
        ("Старт", "+100 доступно", "Доступно 100, накопления 0"),
        ("План", "50 / 25 / 20", "Свободный остаток плана 5"),
        ("Задание", "+15 доступно", "Доступно 115"),
        ("Покупки", "−35 нужное; −25 желаемое", "Доступно 55"),
        ("Взнос", "−20 доступно; +20 накопления", "Доступно 35, накопления 20"),
        ("Итог", "+10 нужное; +5 план; +5 сбережения", "Рост +20"),
    ],
    widths=[1.2, 2.6, 3.4],
    font_size=8.0,
)
paragraph(doc, "Дополнительная награда задания увеличивает доступный баланс, но не меняет уже подтвержденный план. Поэтому план и факт остаются сопоставимыми.")

add_page_break(doc)

# 6 Matrix
section_title(doc, "6", "Матрица соответствия")
paragraph(doc, "Статусы проверены по текущему коду. «Частично» означает неполное выполнение формального критерия или отсутствие объективного измерения.")

matrix_rows = [
    ("2.5.1", "Первый запуск, гостевой профиль, повторная подсказка", "Реализовано", "OnboardingScreens.kt; Intro, Help"),
    ("2.5.2", "Внешний вид и имя питомца", "Реализовано", "OnboardingScreens.kt; CreatePetScreen"),
    ("2.5.3", "Сводный Home и вход во все разделы", "Реализовано", "HomeScreen.kt; FinniApp.kt"),
    ("2.5.4", "Игровая валюта и объяснимые начисления", "Реализовано", "FinniRepository.startPeriodLocked, completeTask"),
    ("2.5.5", "План трех направлений, лимит, остаток, план/факт", "Реализовано", "BudgetScreen.kt; EconomyRules.validatePlan"),
    ("2.5.6", "Покупки двух типов, подтверждение, история, защита баланса", "Реализовано", "ShopScreen.kt; FinniRepository.purchase"),
    ("2.5.7", "Цель, прогресс, взносы, срок, подтверждение снятия", "Реализовано", "SavingsScreen.kt"),
    ("2.5.8", "6 заданий по 3 темам, действие и объяснение", "Реализовано", "TaskScreens.kt; GameCatalog.tasks"),
    ("2.5.9", "Причинная обратная связь и путь восстановления", "Реализовано", "GameResult messages; retryText"),
    ("2.5.10", "3 стадии и рост по совокупности решений", "Реализовано", "GrowthStage; EconomyRules.growthAward"),
    ("2.5.11", "История, прогресс цели, задания и словарь", "Реализовано", "ProgressScreen"),
    ("2.5.12", "Барьер взрослого и нейтральный прогресс", "Реализовано", "AdultGateScreen; AdultScreen"),
    ("2.5.13", "Сохранение и сбрасываемый демопрофиль", "Реализовано", "SQLite; createDemoProfile; resetDemoProfile"),
    ("2.5.14", "Расширяемый контент", "Реализовано с ограничением", "GameCatalog; 3 встроенных типа механик"),
    ("2.6", "≥9 визуальных комбинаций", "Реализовано", "3 персонажа × 3 цветовых фона; unit test"),
    ("2.6", "≥5 последовательных периодов", "Реализовано", "startNextPeriod; period_1..period_5"),
    ("2.6", "≥6 заданий / 3 темы", "Реализовано", "GameCatalog.tasks; unit test"),
    ("2.6", "≥8 покупок двух типов", "Реализовано", "8 позиций GameCatalog.shopItems; unit test"),
    ("2.6", "≥3 целей", "Реализовано", "4 цели; unit test"),
    ("2.6", "≥3 стадий", "Реализовано", "GrowthStage; unit test"),
    ("3.1", "Android 8+, offline, необязательный шагомер", "Реализовано", "minSdk 26; Manifest без INTERNET"),
    ("3.3", "Подписанный release APK", "Не завершено", "Нет signingConfig и финального signed APK"),
    ("3.4", "Автотесты ключевой логики", "Частично", "EconomyRulesTest; нет UI и migration tests"),
    ("3.4", "Старт ≤5 с, отклик ≤1 с, стабильность", "Частично", "Smoke-тест есть; формальных замеров нет"),
    ("3.5", "Нет аккаунтов, рекламы и реальных платежей", "Реализовано", "Manifest и локальная архитектура"),
    ("3.6", "Детский UX и доступность", "Частично", "Настройки есть; нужен TalkBack/font-scale audit"),
]
add_table(
    doc,
    ["Пункт", "Требование", "Статус", "Экран модуль тест"],
    matrix_rows,
    widths=[0.55, 3.0, 1.15, 2.5],
    font_size=6.9,
)

add_page_break(doc)

# 7 Education
section_title(doc, "7", "Карта образовательного контента")
education_rows = [
    ("Планирование", "Распределить доход", "Разложи 60 монет", "Забота ≥30; Мечта ≥10; сумма ≤60", "Сначала защити нужное и оставь часть для мечты"),
    ("Планирование", "Пересобрать план", "Разрядилась батарея", "Забота ≥35; Мечта ≥10; сумма ≤60", "При неожиданности желаемое можно перенести"),
    ("Сбережения", "Регулярно откладывать", "Дорога к мотоциклу", "≥3 взносов; сумма ≥30", "Несколько маленьких шагов приближают цель"),
    ("Сбережения", "Ценить регулярность", "Сохранить мечту", "≥3 взносов; сумма ≥30", "Регулярность делает путь понятнее"),
    ("Покупки", "Ставить нужное первым", "Заряди героя", "Масло + батарея; сумма ≤50", "Сначала то, без чего герой не работает"),
    ("Покупки", "Считать корзину", "Набор заботы", "≥2 нужных; сумма ≤55", "Проверь цену всей корзины"),
    ("План и факт", "Оценивать решение", "Итог периода", "Сравнить три направления", "План — намерение, факт — результат"),
    ("Цель", "Оценивать остаток", "Моя мечта", "Цена − накопления; темп взносов", "Регулярный взнос делает срок понятнее"),
    ("Ограничения", "Не уходить в минус", "Недоступная покупка", "Выбрать дешевле, задание или перенос", "Не купить сейчас — нормально"),
]
add_table(
    doc,
    ["Тема", "Навык", "Сценарий", "Правильная логика", "Объяснение ребенку"],
    education_rows,
    widths=[1.0, 1.25, 1.35, 1.65, 1.95],
    font_size=6.9,
)

doc.add_paragraph(style="Heading 2").add_run("Принципы подачи")
for text in (
    "Сначала действие, затем объяснение финансового и игрового результата.",
    "Короткие предложения и суммы, доступные ребенку 7–11 лет.",
    "Ошибочный вариант можно повторить без потери ранее достигнутого прогресса.",
    "Задания требуют распределения, выбора или сборки корзины, а не только ответа на тест.",
    "Каталог контента отделен от UI, а версии сохраняются в периодах и попытках.",
):
    bullet(doc, text)

add_page_break(doc)

# 8 UX
section_title(doc, "8", "UX UI и доступность")
doc.add_paragraph(style="Heading 2").add_run("Обоснование интерфейса")
ux_rows = [
    ("Три кармашка", "Детские названия сохраняют смысл обязательного, желаемого и накоплений."),
    ("Один следующий шаг", "Home выделяет действие, необходимое для продолжения цикла."),
    ("Решение до подтверждения", "Цена, категория, эффект и остаток показываются заранее."),
    ("Безопасная ошибка", "Нет стыда и необратимой потери; есть понятный способ исправления."),
    ("Визуальная причинность", "Состояние и экипировка героя меняются сразу после действия."),
    ("Раздел взрослого", "Барьер уменьшает риск случайного сброса и удаления."),
]
add_table(doc, ["Решение", "Почему"], ux_rows, widths=[2.0, 5.2], font_size=8.3)

doc.add_paragraph(style="Heading 2").add_run("Реализовано")
for text in (
    "Основной текст 16 sp; заголовки 17–30 sp; вторичный текст местами 14–15 sp.",
    "Ключевые кнопки игрового сценария высотой 56 dp.",
    "Категории и статусы передаются текстом и иконкой, а не только цветом.",
    "Растровые изображения имеют contentDescription.",
    "Критические сообщения продублированы текстом.",
    "Настройки «Звуки» и «Меньше анимации» сохранены локально.",
    "Единая стрелка назад и подтверждение заметно меняющих прогресс действий.",
):
    bullet(doc, text)

doc.add_paragraph(style="Heading 2").add_run("Требует проверки")
for text in (
    "TalkBack, порядок фокуса и доступные названия всех интерактивных элементов.",
    "Масштаб шрифта 1.3–2.0 и ширина 360 dp без обрезания.",
    "Контраст вторичных подписей по WCAG.",
    "Применение soundEnabled и reducedMotion к будущим звукам и анимациям.",
):
    bullet(doc, text)

add_page_break(doc)

# 9 Privacy
section_title(doc, "9", "Разрешения и данные")
doc.add_paragraph(style="Heading 2").add_run("Android-разрешения")
add_table(
    doc,
    ["Разрешение feature", "Назначение", "Без него"],
    [
        ("ACTIVITY_RECOGNITION", "Системный счетчик шагов на Android 10+", "Игра работает без шагов"),
        ("FOREGROUND_SERVICE", "Фоновый сервис шагомера", "Нет фонового учета"),
        ("FOREGROUND_SERVICE_HEALTH", "Тип health service на новых Android", "Фоновая работа ограничена"),
        ("sensor.stepcounter required=false", "Необязательная аппаратная возможность", "Устройство поддерживается"),
    ],
    widths=[2.25, 2.75, 2.2],
    font_size=8.0,
)
paragraph(doc, "Не запрашиваются INTERNET, камера, микрофон, геолокация, контакты и Bluetooth.")

doc.add_paragraph(style="Heading 2").add_run("Локально сохраняется")
for text in (
    "игровое имя профиля и имя питомца;",
    "внешность, экипировка и показатели героя;",
    "планы, журнал валюты, покупки, цели и накопления;",
    "попытки заданий, итоги периодов и события состояния;",
    "дельта шагов, остаток до следующего списания и последнее показание счетчика;",
    "настройки звука и уменьшения анимации.",
):
    bullet(doc, text)

doc.add_paragraph(style="Heading 2").add_run("Не собирается")
paragraph(doc, "Реальное имя, телефон, e-mail, дата рождения, геолокация, платежные реквизиты, семейный бюджет, рекламный идентификатор и аналитика. Передачи данных нет. Android backup отключен.")

doc.add_paragraph(style="Heading 2").add_run("Удаление профиля")
reset_numbers()
number(doc, "На Home нажать «Взрослым» и решить 17 + 6.")
number(doc, "Нажать «Удалить локальный профиль».")
number(doc, "Прочитать предупреждение и подтвердить.")
paragraph(doc, "Удаление корневого profiles каскадно очищает игровые таблицы. SharedPreferences с двумя настройками остаются до очистки данных приложения средствами Android. Демопрофиль можно отдельно сбросить к исходному состоянию.")

add_page_break(doc)

# 10 Testing
section_title(doc, "10", "Тестирование")
doc.add_paragraph(style="Heading 2").add_run("Автоматизированные проверки")
code_block(doc, ["./gradlew testDebugUnitTest", "./gradlew lintDebug"])
paragraph(doc, "Проверено 29 сентября 2026 года после добавления наушников: testDebugUnitTest, lintDebug и assembleDebug завершились BUILD SUCCESSFUL. Проверенная ранее команда assembleRelease создала неподписанный APK размером около 32 МБ.")
paragraph(doc, "EconomyRulesTest проверяет бюджет, рост, стадии, объем каталога, транспортные цели, эффекты экипировки, расчет шагов и сочетание порогов.")

doc.add_paragraph(style="Heading 2").add_run("Ручные тест-кейсы")
test_rows = [
    ("TC-01", "Первый запуск", "Гостевой профиль и Home без регистрации"),
    ("TC-02", "Демопрофиль", "Состояние воспроизводимо после сброса"),
    ("TC-03", "План 50/25/20", "Подтвержден; остаток 5"),
    ("TC-04", "Сумма плана > базы", "Подтверждение блокируется"),
    ("TC-05", "Верное и неверное задание", "Объяснение; награда только за успех"),
    ("TC-06", "Обязательная покупка", "Баланс, история и состояние обновлены"),
    ("TC-07", "Недостаточно средств", "Нет списания; показан дефицит и варианты"),
    ("TC-08", "Цель и взнос", "Баланс уменьшается; накопления растут"),
    ("TC-09", "Возврат 10", "До подтверждения виден новый остаток и срок"),
    ("TC-10", "Завершение периода", "План/факт, объяснение, 0–20 роста"),
    ("TC-11", "30 и 70 очков", "Смена стадии"),
    ("TC-12", "Перезапуск", "Состояние сохранено"),
    ("TC-13", "Разрешение шагомера", "Новые шаги без задвоения"),
    ("TC-14", "Обувь и рюкзак", "Порог 1000/1500/2000 и новый арт"),
    ("TC-15", "Взрослый и удаление", "Барьер; подтверждение удаления"),
]
add_table(doc, ["ID", "Проверка", "Ожидаемый результат"], test_rows, widths=[0.7, 2.2, 4.3], font_size=7.8)

doc.add_paragraph(style="Heading 2").add_run("Физическое устройство")
add_table(
    doc,
    ["Пункт", "Результат"],
    [
        ("Устройство", "ADB serial ABPJJV5A23H00266; ранее определялось как LGN-LX1"),
        ("Установка debug APK", "Пройдена; adb install -r возвращал Success"),
        ("Повторное обновление", "Пройдено после изменений"),
        ("Запуск интерфейса", "Пройден в ручном smoke-тесте"),
        ("Сохранение при install -r", "Пройдено без очистки данных"),
        ("UI automation", "Не выполнялась"),
        ("Signed release на чистой установке", "Требуется финальный прогон"),
        ("Замеры ≤5 с и ≤1 с", "Не выполнялись"),
    ],
    widths=[2.5, 4.7],
    font_size=8.1,
)

# 11 Limitations
section_title(doc, "11", "Ограничения и развитие")
doc.add_paragraph(style="Heading 2").add_run("Известные ограничения")
limitations = [
    "Нет настроенной release-подписи и финального signed APK.",
    "Нет автоматизированных UI-, accessibility- и migration-тестов.",
    "Репозиторий доверяет actionsSnapshot задания; production-валидация должна быть доменной.",
    "Поддерживается один локальный профиль.",
    "Каталог компилируется в APK и не обновляется отдельным пакетом.",
    "Не для всех сочетаний аксессуаров есть единое растровое изображение.",
    "Цвет меняет фон аватара; нет отдельных растровых вариантов всех девяти сочетаний.",
    "Шаги зависят от TYPE_STEP_COUNTER и поведения прошивки.",
    "Планшеты, landscape, TalkBack и крупный шрифт требуют полного аудита.",
    "Звуков нет; настройки предусмотрены под дальнейшее развитие.",
    "Права на изображения нужно подтвердить до публикации.",
    "В репозитории нет отдельного LICENSE.",
]
for item in limitations:
    bullet(doc, item)

doc.add_paragraph(style="Heading 2").add_run("До финальной сдачи")
for item in (
    "Выпустить signed release APK без хранения секретов в Git.",
    "Пройти сквозной сценарий на чистой установке и записать видео до 3 минут.",
    "Провести аудит TalkBack, font scale, 360 dp и контраста.",
    "Добавить UI-, migration- и доменные тесты заданий.",
    "Оформить права на ассеты, LICENSE и черновик карточки RuStore.",
):
    bullet(doc, item)

doc.add_paragraph(style="Heading 2").add_run("После MVP")
for item in (
    "Перейти к многомодульной структуре и Room.",
    "Вынести каталог в версионируемые JSON-пакеты со схемой.",
    "Добавить новые безопасные сценарии непредвиденных расходов.",
    "Поддержать несколько локальных профилей и планшеты.",
    "Подключить CI с тестами и безопасной подписью через секреты.",
):
    bullet(doc, item)

add_page_break(doc)

# 12 Licenses
section_title(doc, "12", "Сторонние компоненты и лицензии")
license_rows = [
    ("Kotlin", "Язык и stdlib", "Apache License 2.0"),
    ("Gradle", "Сборка", "Apache License 2.0"),
    ("Android Gradle Plugin / SDK", "Android toolchain", "Условия Android SDK; проверить NOTICE"),
    ("AndroidX Core Activity Lifecycle", "Android API и lifecycle", "Apache License 2.0"),
    ("Jetpack Compose Material 3", "UI", "Apache License 2.0"),
    ("Kotlin Coroutines", "Flow и фоновые операции", "Apache License 2.0"),
    ("JUnit 4.13.2", "Unit-тесты", "Eclipse Public License 1.0"),
    ("System sans-serif", "Типографика", "Поставляется Android; файл не включен"),
    ("Emoji", "Вспомогательные глифы", "Рендерятся системным шрифтом"),
    ("PNG в drawable-nodpi", "Персонажи, предметы, цели", "Проектные / сгенерированные; право распространения подтвердить"),
    ("Звуки", "Не используются", "Файлы отсутствуют"),
]
add_table(doc, ["Компонент", "Назначение", "Лицензия статус"], license_rows, widths=[2.2, 2.3, 2.7], font_size=7.7)

callout(
    doc,
    "Перед публикацией",
    "Добавить LICENSE проекта, сохранить тексты применимых лицензий и составить реестр происхождения каждого изображения. Настоящая таблица не заменяет юридическую проверку прав на медиа.",
    fill=PALE_ORANGE,
    accent=ORANGE,
)

doc.add_paragraph(style="Heading 2").add_run("Где проверить версии")
for path in (
    "app/build.gradle.kts",
    "gradle/libs.versions.toml",
    "gradle/wrapper/gradle-wrapper.properties",
    "app/src/main/res/drawable-nodpi",
):
    bullet(doc, path)

add_page_break(doc)

# Appendix
section_title(doc, "А", "Сквозной демонстрационный сценарий")
demo_steps = [
    "Запустить финальную сборку и выбрать «Демо для эксперта».",
    "Проверить локальный профиль, героя, 100 монет и сводный Home.",
    "Распределить 50 / 25 / 20 и подтвердить бюджет.",
    "Выполнить задание и получить объяснимую награду.",
    "Купить один обязательный и один желаемый предмет.",
    "Попробовать покупку при нехватке и проверить подсказку.",
    "Выбрать цель, внести 20 и открыть подтверждение возврата 10.",
    "Разрешить шагомер и проверить изменение прогресса шагов.",
    "Завершить период и объяснить план/факт и рост.",
    "Начать следующий период; повторить до смены стадии.",
    "Перезапустить приложение и подтвердить сохранение.",
    "Открыть раздел взрослого, решить 17 + 6 и сбросить демопрофиль.",
]
reset_numbers()
for step in demo_steps:
    number(doc, step)

doc.add_paragraph(style="Heading 2").add_run("Ссылки на исходники")
references = [
    ("README", "README.md"),
    ("Сборка", "app/build.gradle.kts; gradle/libs.versions.toml"),
    ("Manifest", "app/src/main/AndroidManifest.xml"),
    ("Навигация", "app/src/main/java/com/mikhailskiy/finni/ui/FinniApp.kt"),
    ("Экономика", "app/src/main/java/com/mikhailskiy/finni/data/FinniRepository.kt"),
    ("Формулы", "app/src/main/java/com/mikhailskiy/finni/domain/GameModels.kt"),
    ("Контент", "app/src/main/java/com/mikhailskiy/finni/domain/GameCatalog.kt"),
    ("Схема", "app/src/main/java/com/mikhailskiy/finni/data/FinniDatabase.kt"),
    ("Тесты", "app/src/test/java/com/mikhailskiy/finni/EconomyRulesTest.kt"),
]
add_table(doc, ["Область", "Путь"], references, widths=[1.5, 5.7], font_size=8.0)

doc.add_paragraph(style="Heading 2").add_run("Вывод")
callout(
    doc,
    "Готовность к демонстрации",
    "Прототип покрывает основной образовательный и игровой цикл, содержит восемь товаров, хранит прогресс локально и устанавливался на физическое устройство. Для финальной готовности остается выпустить подписанный release APK и провести чистый сквозной прогон.",
    fill=MINT,
    accent=GREEN,
)

# Document metadata and final save
doc.core_properties.title = "Финни Три кармашка — техническая документация"
doc.core_properties.subject = "Сборка, архитектура, данные, требования, правила, образовательный контент и тестирование"
doc.core_properties.author = "Команда проекта Финни"
doc.core_properties.keywords = "Android, Kotlin, Compose, финансовая грамотность, Финни"

OUTPUT.parent.mkdir(parents=True, exist_ok=True)
doc.save(OUTPUT)
print(OUTPUT)
