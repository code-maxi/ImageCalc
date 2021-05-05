import javafx.application.Application

class LaunchUI {
    companion object {
        lateinit var params: Array<String>
        @JvmStatic
        fun main(args: Array<String>) {
            params = args// + "/home/maximilian/Programmieren/aster/src/main/resources/images"
            Application.launch(ImageCalcStart::class.java, *args)
        }
    }
}