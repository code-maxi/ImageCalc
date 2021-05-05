import javafx.application.Platform
import javafx.collections.ObservableList
import javafx.geometry.Orientation
import javafx.geometry.Pos
import javafx.scene.canvas.Canvas
import javafx.scene.canvas.GraphicsContext
import javafx.scene.control.*
import javafx.scene.image.Image
import javafx.scene.input.KeyCode
import javafx.scene.input.MouseButton
import javafx.scene.layout.*
import javafx.scene.paint.Color
import javafx.scene.text.Font
import javafx.stage.DirectoryChooser
import javafx.stage.FileChooser
import javafx.stage.Stage
import tornadofx.*
import java.io.*
import java.util.*
import kotlin.collections.ArrayList
import kotlin.concurrent.thread
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.system.exitProcess


lateinit var stage: Stage
lateinit var calc: ImageCalc

class ImageCalcStart : App(ImageCalc::class) {
    override fun start(s: Stage) {
        super.start(s)
        s.setOnCloseRequest { exitProcess(0) }
        s.icons.add(Image("icon.png"))
        s.scene.stylesheets.add("./style.css")
        s.isMaximized = true
        stage = s
    }
}

data class Point(var x: Double, var y: Double) : Serializable {
    infix fun length(p: Point) = kotlin.math.sqrt((p.x - x) * (p.x - x) + (p.y - y) * (p.y - y))
    fun json() = "{ \"x\": $x, \"y\": $y }"

    fun l() = kotlin.math.sqrt(x * x + y * y)
    fun a() = atan2(y, x)
    fun e() = this / l()
    fun out() = -y point x

    infix fun distance(v: Point) = kotlin.math.sqrt(
        (v.x - x) * (v.x - x) +
                (v.y - y) * (v.y - y)
    )

    infix fun vec(v: Point) = Point(v.x - x, v.y - y)

    operator fun times(s: Double) = Point(x * s, y * s)
    operator fun div(s: Double) = Point(x / s, y / s)
    operator fun plus(v: Point) = Point(x + v.x, y + v.y)
    operator fun minus(v: Point) = Point(x - v.x, y - v.y)

    operator fun div(s: Point) = Point(x / s.x, y / s.y)
    operator fun times(s: Point) = Point(x * s.x, y * s.y)

    fun multiplySkalar(that: Point) = x * that.x + y * that.y
    fun vektorprodukt(v: Point) = x * v.y - y * v.x

    infix fun between(b: Point) =
        acos(multiplySkalar(b) / (l() * b.l()))

    fun turn(d: Double) = Point(
        x * cos(d) - y * sin(d),
        y * cos(d) + x * sin(d)
    )
    fun turn(d: Double, p: Point) = (this - p).turn(d) + p

    companion object {
        fun toXY(a: Double, l: Double) = Point(cos(a) * l, sin(a) * l)
    }
}
infix fun Double.point(d: Double) = Point(this, d)
infix fun Point.delta(p: Point) = p.x - x point p.y - y

val acceptedExtensions = arrayOf("png", "PNG", "jpg", "JPG", "JPEG")

data class TreeFile(val file: File? = null, val name: String = file?.name ?: "Geöffnete Dateien") {
    override fun toString() = name
}

data class ImagePathData(val name: String, val color: PathColor, val group: String, val points: Array<Point>) : Serializable
data class ImagePathsData(val paths: Array<ImagePathData>) : Serializable {
    fun code(): String {
        val baos = ByteArrayOutputStream()
        val oos = ObjectOutputStream(baos)
        oos.writeObject(this)
        oos.close()
        return Base64.getEncoder().encodeToString(baos.toByteArray())
    }
}

fun decode(s: String): ImagePathsData? {
    return try {
        val data = Base64.getDecoder().decode(s)
        val ois = ObjectInputStream(ByteArrayInputStream(data))
        ois.readObject() as ImagePathsData
    } catch (e: Exception) { null }
}

class ImageCalc : View("ImageCalc") {
    lateinit var file: File
    lateinit var canvas: CalcCanvas
    lateinit var img: Image

    val padding = 200.0

    var snapRadius = 12.0

    var autoReload = true

    lateinit var slider: Slider

    lateinit var ta: TextArea
    lateinit var te: TextArea
    lateinit var tc: TextArea

    lateinit var posCheck: CheckBox
    lateinit var lengthCheck: CheckBox
    lateinit var numberCheck: CheckBox
    lateinit var lineCheck: CheckBox
    lateinit var pointCheck: CheckBox
    lateinit var snapCheck: CheckBox
    lateinit var onlyCurrent: CheckBox

    lateinit var pathName: TextField
    lateinit var pathGroup: TextField

    lateinit var visiblePath: CheckBox

    lateinit var wField: TextField
    lateinit var hField: TextField
    lateinit var txField: TextField
    lateinit var tyField: TextField
    lateinit var nachkomma: TextField

    lateinit var removePointField: TextField

    lateinit var scroll: ScrollPane

    lateinit var tree: TreeView<TreeFile>

    var snap: Point? = null

    var pathCount = 1
    val paths = observableListOf<ImagePath>()
    lateinit var pathView: ListView<ImagePath>
    var selectedPath: ImagePath? = null
    var selectedPaths = observableListOf<ImagePath>()

    var axisPoint = Point(0.0, 0.0)

    fun sx() = (wField.text.toIntOrNull() ?: (wField.text.toDoubleOrNull() ?: 1.0) * img.width).toDouble() / img.width
    fun sy() = (hField.text.toIntOrNull() ?: (hField.text.toDoubleOrNull() ?: 1.0) * img.height).toDouble() / img.height

    fun st(t: String, m: Double) = (if (t == "x") txField else tyField).text.let {
        if (it.contains(".")) (it.toDoubleOrNull() ?: 0.0) * m
        else ("$it.0").toDoubleOrNull() ?: 0.0
    }

    fun fr(p: Point) = p.x * img.width * sx() + st("x", img.width * sx()) point (p.y * img.height * sy() + st("y", img.height * sy()))
    fun rr(p: Point) = p.x * w() point p.y * h()

    fun rn(p: Any?) = if (p is Point) rr(p) else null

    fun Point.f() = fr(this)
    fun Point.r() = rr(this)

    fun pathsFiltered() = paths.filter { it.visible }.arrayList()

    override val root = splitpane {
        orientation = Orientation.HORIZONTAL
        vgrow = Priority.ALWAYS
        hgrow = Priority.ALWAYS
        style { padding = box(10.px) }

        vbox {
            style { padding = box(5.px) }

            minWidth = 410.0
            maxWidth = 600.0

            spacing = 15.0
            hbox {
                alignment = Pos.CENTER_LEFT
                spacing = 5.0
            }
            slider = slider(0.0, 1.0, 0.5, Orientation.HORIZONTAL) {
                prefWidth = 300.0
                valueProperty().onChange {
                    canvas.setSizes()
                }
            }
            flowpane {
                hgap = 10.0
                vgap = 10.0
                posCheck = checkbox("Position") { isSelected = true; action { canvas.paint() } }
                lengthCheck = checkbox("Länge") { isSelected = true; action { canvas.paint() } }
                numberCheck = checkbox("Nummer") { isSelected = true; action { canvas.paint() } }
                lineCheck = checkbox("Linie") { isSelected = true; action { canvas.paint() } }
                pointCheck = checkbox("Punkte") { isSelected = true; action { canvas.paint() } }
                onlyCurrent = checkbox("Nur Aktuelles") { isSelected = true; action { canvas.paint() } }
                snapCheck = checkbox("Snap") { isSelected = true; action { canvas.paint() } }
            }

            hbox {
                spacing = 10.0
                wField = textfield { textProperty().onChange { reload() }; prefWidth = 60.0; promptText = "Breite" }
                hField = textfield { textProperty().onChange { reload() }; prefWidth = 60.0; promptText = "Höhe" }
                txField = textfield { textProperty().onChange { reload() }; prefWidth = 60.0; promptText = "X" }
                tyField = textfield { textProperty().onChange { reload() }; prefWidth = 60.0; promptText = "Y" }
                nachkomma = textfield { textProperty().onChange { reload() }; prefWidth = 60.0; promptText = "x.[n]" }
            }

            splitpane(Orientation.VERTICAL) {
                vgrow = Priority.ALWAYS
                hgrow = Priority.ALWAYS

                te = textarea("""
                Dies ist ein Beilspiel-Codepattern. 
                Es zeigt die Eigenschaften des aktuellen Bildes und 
                die Pfade mit deren Punkten...
                
                Name: #{name}
                Pfad: #{path}
                Typ: #{extension}
                Auflösung: #{iw}px x #{ih}px
                    
                <#{n} (Farbe: #{c} | Gruppe: '#{g}'):
                    `Punkt #{i}: (#{x} | #{y})
                    `
                >
                """.trimIndent()) {
                    hgrow = Priority.ALWAYS
                    vgrow = Priority.ALWAYS

                    prefHeight = 300.0

                    promptText = "Code-Pattern für die Erzeugung..."
                    font = Font.font("monospace")
                    textProperty().onChange { reload() }
                }
                ta = textarea {
                    hgrow = Priority.ALWAYS
                    vgrow = Priority.ALWAYS

                    promptText = "Ergebnis vom Code-Pattern..."
                    isEditable = false
                    font = Font.font("monospace")
                }
                vbox {
                    spacing = 5.0
                    hgrow = Priority.ALWAYS
                    vgrow = Priority.ALWAYS

                    maxHeight = 200.0
                    prefHeight = 150.0

                    tc = textarea {
                        hgrow = Priority.ALWAYS
                        vgrow = Priority.ALWAYS

                        promptText = "Codierte Daten"
                        font = Font.font("monospace")
                        isWrapText = true

                        setOnKeyPressed {
                            if (it.code == KeyCode.S && it.isControlDown) {
                                decodePaths()
                                reload()
                            }
                        }
                    }

                    button("Aktualisieren") {
                        action {
                            decodePaths()
                            reload()
                        }
                    }
                }
            }
        }

        scroll = scrollpane {
            vgrow = Priority.ALWAYS
            hgrow = Priority.ALWAYS

            this += CalcCanvas()
        }

        splitpane(Orientation.VERTICAL) {
            minWidth = 300.0
            prefWidth = 300.0
            maxWidth = 500.0

            style { padding = box(5.px) }

            vbox {
                vgrow = Priority.ALWAYS
                hgrow = Priority.ALWAYS

                style { padding = box(5.px) }
                spacing = 5.0

                tree = treeview(TreeItem(TreeFile()).apply { isExpanded = true }) {
                    vgrow = Priority.ALWAYS
                    hgrow = Priority.ALWAYS

                    selectionModel.selectedItems.onChange {
                        selectedValue?.let {
                            if (it.file?.isDirectory == false) {
                                file = it.file
                                if (autoReload) reloadImage()
                            }
                        }
                    }
                }

                hbox {
                    alignment = Pos.CENTER_LEFT
                    spacing = 5.0
                    button("Entfernen") {
                        action { tree.selectionModel.selectedItems.forEach {
                            it.parent?.let { p ->
                                Platform.runLater { p.children.remove(it) }
                            }
                        } }
                    }
                    val folderCheck = CheckBox("Ordner")
                    button("Öffnen") { action { loadImagesDialog(false, folderCheck.isSelected) } }
                    this += folderCheck
                }
            }

            vbox {
                vgrow = Priority.ALWAYS
                hgrow = Priority.ALWAYS

                spacing = 5.0
                pathView = listview {
                    vgrow = Priority.ALWAYS
                    hgrow = Priority.ALWAYS

                    prefHeight = 200.0

                    multiSelect(true)

                    items.bind(paths) { it }
                    selectionModel.selectedItemProperty().onChange {
                        selectedPath = selectedItem
                        selectedPaths = selectionModel.selectedItems
                        Platform.runLater {
                            selectedPath?.let {
                                visiblePath.isSelected = it.visible
                                pathName.text = it.name
                                pathGroup.text = it.group
                            }
                        }
                        reloadLast()
                        canvas.paint()
                    }
                    items.onChange { reload() }
                }

                visiblePath = checkbox("Sichtbar") {
                    action {
                        Platform.runLater {
                            pathView.selectionModel.selectedItems?.forEach { it.visible = isSelected }
                            pathView.refresh()
                        }
                        reload()
                    }
                }

                hbox {
                    spacing = 5.0
                    hgrow = Priority.ALWAYS
                    pathName = textfield {
                        hgrow = Priority.ALWAYS
                        promptText = "Name"
                        textProperty().onChange {
                            selectedPath?.let {
                                it.name = text
                                reload()
                            }
                        }
                    }
                    pathGroup = textfield {
                        hgrow = Priority.ALWAYS
                        promptText = "Gruppe"
                        textProperty().onChange {
                            if (text.isNotEmpty()) {
                                selectedPaths.forEach { g -> g.group = text }
                                reload()
                            }
                        }
                    }
                }

                hbox {
                    spacing = 5.0

                    button("Pfad entfernen") { action {
                        selectedPath.let { paths.remove(it); reloadLast() }
                    } }

                    removePointField = textfield {
                        prefWidth = 50.0
                    }
                    button("Punkt entfernen") { action { removePointField.text.toIntOrNull()?.let {
                        removeLastPoint(i = it)
                    } } }
                }
            }
        }
    }

    init {
        calc = this

        thread {
            if (LaunchUI.params.isNotEmpty()) LaunchUI.params.forEach {
                Regex("(?<text>[^:]+)").find(it).let { m ->
                    thread { loadImage(File(m!!.groups["text"]!!.value)) }
                }
            }
        }
    }

    private fun reloadImage() {
        thread {
            img = Image(FileInputStream(file))

            canvas.setSizes()
            scroll.apply {
                Platform.runLater {
                    prefWidth = canvas.width
                    prefHeight = canvas.height
                }
            }
            reload()
        }
    }

    fun Double.format() = nachkomma.text.toIntOrNull()?.let { "%.${it}f".format(this).replace(",", ".") } ?: toInt().toString()
    
    fun reload(refresh: Boolean = true) {
        Platform.runLater {
            fun String.form() = this
                .replace("~", "\n")
                .replace("–", "    ")

            fun String.rw(r: String, t: String) = replace(Regex("#\\{$r}(\\[w=(?<z>\\d+)])?")) { mr ->
                "${" ".count((mr.groups["z"]?.value?.toInt() ?: t.length) - t.length)}$t"
            }

            var s = te.text

            s = s.replace("\n", "~")

            val range = "(\\[(?<z1>\\d+)?(?<o>-)?(?<z2>\\d+)?])?"

            fun <T> String.rangeString(
                arr: List<T>,
                op: Pair<String, String> = "-" to "-",
                pattern: Regex = "${op.first}$range(?<text>.+?)${op.second}".toRegex(),
                filter: (MatchResult, T) -> Boolean = { _, _ -> true },
                f: (T, String, Int) -> String
            ) = replace(pattern) {
                    val z1 = it.groups["z1"]?.value?.toIntOrNull()
                    val o = it.groups["o"]
                    val z2 = it.groups["z2"]?.value?.toIntOrNull()

                    var st = ""

                    if (o != null && z1 != null && z2 == null) {
                        arr.getOrNull(z1)?.let { e ->
                            if (filter(it, e)) st += f(e, it.groups["text"]!!.value, arr.indexOf(e)) }
                    }
                    else if (o != null && z1 == null && z2 != null) {
                        arr.getOrNull(arr.size-1 - z2)?.let { e ->
                            if (filter(it, e)) st += f(e, it.groups["text"]!!.value, arr.indexOf(e)) }
                    }
                    else if ((o != null && z1 != null && z2 != null) || (o == null && z1 == null && z2 == null)) {
                        arr.forEachIndexed { ind, t ->
                            if (ind >= (z1 ?: 0) && ind < arr.size - (z2 ?: 0) && filter(it, t))
                                st += f(t, it.groups["text"]!!.value, ind)
                        }
                    } else arr.filter { i -> filter(it, i) }.forEachIndexed { ind, t ->
                        st += f(t, it.groups["text"]!!.value, ind)
                    }
                    st
                }

            s = s.rangeString(
                pathsFiltered(),
                pattern = "<$range(\\[(?<group>[\\w;]+)])?(?<text>.+?)>".toRegex(),
                filter = { result, o ->
                    val r = result.groups["group"]?.value?.split(";")
                    r?.contains(o.group) ?: true
                }
            ) { o, t, i ->
                val l = o.points.copyAs { fr(it) }
                val m = l.m()
                val m2 = l.m(-1)
                val ml = l.ml(m)
                val ml2 = l.ml(m)
                t
                    .rangeString(o.points, "`" to "`") { oo, tt, ii ->
                        val p = oo.f()

                        val np = if (ii < o.points.size-1) o.points[ii+1].f() else null

                        val dl = if (np != null) (p delta np).l().format() else "[...]"
                        val da = if (np != null) (p delta np).a().format() else "[...]"
                        val dx = if (np != null) (p delta np).x.format() else "[...]"
                        val dy = if (np != null) (p delta np).y.format() else "[...]"

                        println(ii)

                        tt
                            .rw("x", p.x.format())
                            .rw("y", p.y.format())
                            .rw("dx", dx)
                            .rw("dy", dy)
                            .rw("dl", dl)
                            .rw("da", da)
                            .rw("id", (ii+1).toString())
                            .rw("i", ii.toString())
                    }
                    .rw("i", i.toString())
                    .rw("c", o.color.s)
                    .rw("n", o.name)
                    .rw("g", o.group)
                    .rw("mx", m.x.format())
                    .rw("my", m.y.format())
                    .rw("ml", ml.format())
                    .rw("m2x", m2.x.format())
                    .rw("m2y", m2.y.format())
                    .rw("m2l", ml2.format())
            }

            val l = arrayListOf<Point>()
            paths.forEach { l += it.points.copyAs { fr(it) } }

            val m = l.m()
            val m2 = l.m(-1)
            val ml = l.ml(m)
            val ml2 = l.ml(m)

            s = s
                .rw("iw", img.width.format())
                .rw("ih", img.height.format())
                .rw("path", file.path)
                .rw("name", file.name)
                .rw("extension", file.name)
                .rw("mx", m.x.format())
                .rw("my", m.y.format())
                .rw("ml", ml.format())
                .rw("m2x", m2.x.format())
                .rw("m2y", m2.y.format())
                .rw("m2l", ml2.format())

            s = s.form()

            ta.text = s
            tc.text = codePaths()

            reloadLast()

            if (refresh) pathView.refresh()
            canvas.paint()
        }
    }

    fun removeLastPoint(i: Int? = null, reload: Boolean = true) {
        selectedPath?.points?.let { l ->
            if (l.isNotEmpty()) (i ?: l.size-1).let { if (it >= 0 && it < l.size) l.removeAt(it) }
            else paths.remove(selectedPath)
        }
        if (reload) reload()
    }

    private fun reloadLast() {
        Platform.runLater { selectedPath?.let { removePointField.text = (it.points.size - 1).toString() } }
    }

    fun doAdd(t: String, x: Double, y: Double) {
        when (t) {
            "point" -> selectedPath?.points?.add(snap?.copy() ?: x / w() point y / h())
            "path" -> {
                paths.add(ImagePath("Pfad $pathCount", pathColor(paths.size), "a").apply {
                    points.add(snap?.copy() ?: x / w() point y / h())
                    Platform.runLater { pathView.selectionModel.clearSelection(); pathView.selectionModel.select(this) }
                })
                pathCount ++
            }
        }
    }

    inner class CalcCanvas : Canvas() {
        var mouse: Point? = 0.0 point 0.0
        init {
            this@ImageCalc.canvas = this

            style {
                padding = box(10.px)
            }

            setOnMousePressed {
                when (it.button) {
                    MouseButton.PRIMARY -> doAdd("point", it.x - padding, it.y - padding)
                    MouseButton.SECONDARY -> doAdd("path", it.x - padding, it.y - padding)
                    MouseButton.MIDDLE -> removeLastPoint(reload = false)
                }

                reload()
            }
            setOnMouseExited {
                mouse = null
                paint()
            }
            setOnMouseMoved {
                mouse = it.x - padding point it.y - padding
                snap = null

                fun snap(p: Point) { if (p.r() length mouse!! <= snapRadius) snap = p }
                if (snapCheck.isSelected) {
                    pathsFiltered().forEach { path -> (path.points).forEach { p -> snap(p) } }
                    snap(0.5 point 0.5)
                    snap(axisPoint.x/w() point axisPoint.y/h())
                }

                paint()
            }
            setOnScroll {
                if (it.isControlDown) Platform.runLater { slider.value += it.deltaY/1400.0 }
            }

            paint()
        }

        fun setSizes() {
            if (::img.isInitialized) {
                Platform.runLater {
                    val w = 2000.0 * slider.value + 300.0
                    width = w + padding*2.0
                    height = img.height/img.width * w + padding*2.0
                }
                paint()
            }
        }

        fun paint() {
            if (::img.isInitialized) {
                Platform.runLater {
                    val gc = graphicsContext2D
                    gc.apply {
                        save()

                        axisPoint = Point(
                            0.0 - st("x", w()),
                            0.0 - st("y", h())
                        )

                        fill = Color.WHITE
                        stroke = Color.BLACK
                        lineWidth = 1.0
                        fillRect(0.0, 0.0, width, height)

                        translate(padding, padding)

                        strokeRect(0.0, 0.0, w(), h())
                        drawImage(img, 0.0, 0.0, w(), h())

                        stroke = Color.GREEN
                        lineWidth = 1.0
                        beginPath()
                        moveTo(w()/2.0, 0.0)
                        lineTo(w()/2.0, h())
                        moveTo(0.0, h()/2.0)
                        lineTo(w(), h()/2.0)
                        stroke()

                        stroke = Color.RED
                        lineWidth = 2.0
                        beginPath()
                        moveTo(axisPoint.x, 0.0)
                        lineTo(axisPoint.x, h())
                        moveTo(0.0, axisPoint.y)
                        lineTo(w(), axisPoint.y)
                        stroke()

                        fill = Color.rgb(255,0,0, 0.5)
                        fillOval(axisPoint.x - 5.0, axisPoint.y - 5.0, 10.0, 10.0)

                        pathsFiltered().forEach { it.paint(gc) }

                        mouse?.let { mm ->
                            val it = mm / (w() point h())
                            paintBadge(mm.x + 15.0 point mm.y, "${it.f().x.format()} | ${it.f().y.format()}")
                        }

                        snap?.let {
                            fill = Color.rgb(0, 0, 255, 0.3)
                            fillOval(it.r().x - snapRadius, it.r().y - snapRadius, snapRadius*2.0, snapRadius*2.0)
                        }

                        restore()
                    }
                }
            }
        }
    }

    private fun loadImagesDialog(start: Boolean = false, directory: Boolean) {
        if (directory) DirectoryChooser().let { f ->
            f.title = "Ordner Öffnen..."

            Platform.runLater {
                f.showDialog(stage)?.let {
                    thread { thread { loadImage(it) } }
                } ?: run {
                    if (start) stage.close()
                }
            }
        }
        else FileChooser().let { f ->
            f.title = "Datei Öffnen..."
            f.extensionFilters.add(
                FileChooser.ExtensionFilter(
                    "Bilder (PNG, JPG, JPEG)",
                    "*.png",
                    "*.jpg",
                    "*.JPG",
                    "*.JPEG"
                )
            )

            Platform.runLater {
                f.showOpenMultipleDialog(stage)?.forEach {
                    thread { thread { loadImage(it) } }
                } ?: run {
                    if (start) stage.close()
                }
            }
        }
    }

    private fun loadImage(f: File, p: TreeItem<TreeFile> = tree.root, c: Int = 0) {
        println("${f.absoluteFile} --- $c")

        if (f.isDirectory || acceptedExtensions.contains(f.extension)) Platform.runLater {
            p.children.add(TreeItem(TreeFile(f)).apply {
                isExpanded = false

                if (f.isDirectory) {
                    f.walkTopDown().maxDepth(1).sortedBy { if (it.isDirectory) 0 else 1 }.forEachIndexed { i, b ->
                        if (i != 0) thread {
                            loadImage(b, this, c + 1)
                        }
                    }
                } else {
                    file = f
                    tree.selectionModel.select(this)
                }
            })
        }
    }

    fun w() = canvas.width - padding*2.0
    fun h() = canvas.height - padding*2.0

    fun codePaths() = ImagePathsData(paths.copyAs { it.code() }.toTypedArray()).code()

    fun decodePaths() {
        paths.clear()
        decode(tc.text)?.let {
            println(it)
            it.paths.forEach { paths.add(ImagePath(it.name, it.color, it.group).apply {
                points = it.points.arrayList()
            }) }
        }
    }

    inner class ImagePath(var name: String, val color: PathColor, var group: String) {
        var points = arrayListOf<Point>()
        var visible = true

        override fun toString() = "${ if (visible) "*" else "  " } [$group] $name (${color.s})"

        fun code() = ImagePathData(name, color, group, points.toTypedArray())

        fun paint(gc: GraphicsContext) {
            gc.apply {
                val c = this@ImageCalc.canvas
                val m = if (selectedPath === this@ImagePath) c.mouse else null

                val oc = !onlyCurrent.isSelected || selectedPath == this@ImagePath

                for (i in points.indices) {
                    val p = points[i]
                    val pr = rr(p)
                    val np = if (i < points.size-1) points[i+1] else m?.div(w() point h())
                    val npr = rn(np)

                    if (npr != null) {
                        if (lineCheck.isSelected) {
                            lineWidth = if (selectedPath == this@ImagePath) 7.0 else 5.0
                            stroke = if (selectedPath == this@ImagePath) Color.RED else Color.rgb(0,0,0, 0.5)

                            strokeLine(pr.x, pr.y, npr.x, npr.y)

                            lineWidth = 2.0
                            stroke = color.c

                            strokeLine(pr.x, pr.y, npr.x, npr.y)
                        }

                        val tp = (npr.x - pr.x)/2.0 + pr.x + 10.0 point (npr.y - pr.y)/2.0 + pr.y

                        if (lengthCheck.isSelected && oc) paintBadge(tp, (p.f() length np!!.f()).format())
                    }
                    if (posCheck.isSelected && oc) paintBadge(
                        pr.x + 15.0 point
                                pr.y, "${p.f().x.format()} | ${p.f().y.format()}"
                    )
                    if (numberCheck.isSelected && oc) paintBadge(
                        pr.x - 30.0 point
                                pr.y, i.toString(), Color.rgb(0,0,200, 0.4)
                    )
                }

                fill = color.c
                stroke = Color.BLACK
                lineWidth = 2.0
                val s = 10.0
                if (pointCheck.isSelected) points.forEach { p ->
                    val it = p.r()
                    fillOval(it.x - s/2.0, it.y - s/2.0, s, s)
                    strokeOval(it.x - s/2.0, it.y - s/2.0, s, s)
                }
            }
        }
    }
}

fun GraphicsContext.paintBadge(p: Point, s: String, bc: Color = Color.rgb(0,0,0, 0.3)) {
    fill = bc
    stroke = Color.WHITE
    lineWidth = 1.0

    fillRoundRect(p.x - 5.0, p.y - 15.0, 15.0 + (6.5 * s.length), 23.0, 5.0, 5.0)
    strokeText(s, p.x, p.y)
}

enum class PathColor(val c: Color, val s: String) : Serializable {
    BLUE(Color.rgb(96, 138, 255), "blue"),
    GREEN(Color.LIGHTGREEN, "green"),
    RED(Color.rgb(255, 119, 116), "red"),
    ORANGE(Color.ORANGE, "orange"),
    WHITE(Color.WHITE, "white"),
    YELLOW(Color.YELLOW, "yellow")
}

fun pathColor(i: Int) = when (i % 6) {
    0 -> PathColor.YELLOW
    1 -> PathColor.ORANGE
    2 -> PathColor.BLUE
    3 -> PathColor.GREEN
    4 -> PathColor.WHITE
    else -> PathColor.RED
}

fun String.count(i: Int): String {
    var r = ""
    for (ii in 0 until i) r += this
    return r
}

fun <T> Array<T>.arrayList() = arrayListOf(*this)
inline fun <reified T> List<T>.arrayList() = arrayListOf(*toTypedArray())

fun <T : Any, R : Any> ObservableList<T>.copyAs(co: (T) -> R): ObservableList<R> {
    val a = observableListOf<R>()
    forEach { a += co(it) }
    return a
}
fun <T : Any, R : Any> ArrayList<T>.copyAs(co: (T) -> R): ArrayList<R> {
    val a = arrayListOf<R>()
    forEach { a += co(it) }
    return a
}

fun ArrayList<Point>.ml(p: Point): Double {
    var m = p.copy()
    forEach { if (p length m < p length it) m = it }
    return p length m
}

fun ArrayList<Point>.m(d: Int = 0): Point {
    var p = 0.0 point 0.0
    forEach { p += it }
    p /= (size+d).toDouble()
    return p
}