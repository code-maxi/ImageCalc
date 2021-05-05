import javafx.application.Application

class LaunchUI {
    companion object {
        lateinit var params: Array<String>
        @JvmStatic
        fun main(args: Array<String>) {
            params = args
            Application.launch(ImageCalcStart::class.java, *args)
        }
    }
}