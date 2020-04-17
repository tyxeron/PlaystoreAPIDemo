package ch.jschneider.playstoreapidemo

import android.R.attr.password
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.dragons.aurora.playstoreapiv2.PlayStoreApiBuilder
import com.dragons.aurora.playstoreapiv2.PropertiesDeviceInfoProvider
import java.io.IOException
import java.util.*


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // A device definition is required to log in
        // See resources for a list of available devices
        // A device definition is required to log in
        // See resources for a list of available devices
        val properties = Properties()
        try {
            properties.load(ClassLoader.getSystemResourceAsStream("device-honami.properties"))
        } catch (e: IOException) {
            println("device-honami.properties not found")
            return
        }
        val deviceInfoProvider = PropertiesDeviceInfoProvider()
        deviceInfoProvider.setProperties(properties)
        deviceInfoProvider.setLocaleString(Locale.ENGLISH.toString())

        // Provide valid google account info

        // Provide valid google account info
        val builder: PlayStoreApiBuilder = PlayStoreApiBuilder() // Extend HttpClientAdapter using a http library of your choice
                .setHttpClient(HttpClientAdapterImplementation())
                .setDeviceInfoProvider(deviceInfoProvider)
                .setEmail(email)
                .setPassword(password)
        val api = builder.build()

        // We are logged in now
        // Save and reuse the generated auth token and gsf id,
        // unless you want to get banned for frequent relogins

        // We are logged in now
        // Save and reuse the generated auth token and gsf id,
        // unless you want to get banned for frequent relogins
        api.token
        api.gsfId

        // API wrapper instance is ready

        // API wrapper instance is ready
        val response = api.details("com.cpuid.cpu_z")
    }
}
