package com.estimote.proximityapp

import android.app.Notification
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.Toast
import com.estimote.cloud_plugin.common.EstimoteCloudCredentials
import com.estimote.internal_plugins_api.cloud.proximity.ProximityAttachment
import com.estimote.proximity_sdk.proximity.ProximityObserver
import com.estimote.proximity_sdk.proximity.ProximityObserverFactory
import kotlinx.android.synthetic.main.activity_main.*

/**
 * Proximity SDK example.
 *
 * Requirements:
 * 1. Cloud account with beacon attachment setup.
 * 2. AppId and AppToken of your Estimote Cloud App.
 *
 * @author Estimote Inc. (contact@estimote.com)
 */
class MainActivity : AppCompatActivity() {

    // In order to run this example you need to create an App in your Estimote Cloud account and put here your AppId and AppToken
    // ============
    // THIS WILL NOT COMPILE UNLESS YOU PUT PROPER VALUES. SEE ABOVE FOR MORE DETAILS.
    private val cloudCredentials = EstimoteCloudCredentials(PUT_YOUR_APP_ID_STRING_HERE, PUT_YOUR_APP_TOKEN_STRING_HERE)
    // ============

    // Actions to trigger when proximity conditions are met.
    private val makeMintDeskFilled: (ProximityAttachment) -> Unit = { _ -> mint_image.setImageResource(R.color.mint_cocktail) }
    private val makeMintDeskWhite: (ProximityAttachment) -> Unit = { mint_image.setImageResource(R.color.primary) }
    private val makeBlueberryDeskFilled: (ProximityAttachment) -> Unit = { _ -> blueberry_image.setImageResource(R.color.blueberry_muffin) }
    private val makeBlueberryDeskWhite: (ProximityAttachment) -> Unit = { blueberry_image.setImageResource(R.color.primary) }
    private val makeVenueFilled: (ProximityAttachment) -> Unit = { _ -> venue_image.setImageResource(R.color.icy_marshmallow) }
    private val makeVenueWhite: (ProximityAttachment) -> Unit = { venue_image.setImageResource(R.color.primary) }
    private val displayInfoAboutChangeInVenue: (List<ProximityAttachment>) -> Unit = { attachmentsNearby -> Toast.makeText(this, "Venue - current nearby attachments: ${attachmentsNearby.size}", Toast.LENGTH_SHORT).show() }

    // Notification about pending BLE scanning to display in notification bar
    private lateinit var notification: Notification
    // Estimote's main object for doing proximity observations
    private lateinit var proximityObserver: ProximityObserver
    // Handler to stop the observation. You will get this Handler after starting observation, don't worry.
    private lateinit var observationHandler: ProximityObserver.Handler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // Take a look at NotificationCreator class which handles different OS versions
        notification = NotificationCreator().create(this)
        // Check Location permissions in runtime
        PermissionChecker().checkPermissions(this)
        button_start.setOnClickListener {
            startProximityObservation()
            button_start.visibility = View.GONE
        }
    }


    private fun startProximityObservation() {
        // Create ProximityObserver - don't forget to put your APP ID and APP TOKEN.
        // Also make sure that all your beacons have attachments assigned in Estimote Cloud.
        proximityObserver = ProximityObserverFactory().create(applicationContext, cloudCredentials)
        // The first rule is for the venue in general.
        // All devices in this venue will have the same key,
        // and the actions will be triggered when entering/changing/exiting the venue.
        val venueZone = proximityObserver.zoneBuilder()
                .forAttachmentKeyAndValue("venue", "office")
                .inCustomRange(5.0)
                .withOnEnterAction(makeVenueFilled)
                .withOnExitAction(makeVenueWhite)
                .withOnChangeAction(displayInfoAboutChangeInVenue)
                .create()

        // The next rule is defined for a single desk in your venue - let's call it "Mint desk".
        val mintDeskZone = proximityObserver.zoneBuilder()
                .forAttachmentKeyAndValue("desk", "mint")
                .inNearRange()
                .withOnEnterAction(makeMintDeskFilled)
                .withOnExitAction(makeMintDeskWhite)
                .create()

        // The last rule is defined for another single desk in your venue - the "Blueberry desk".
        val blueberryDeskZone = proximityObserver.zoneBuilder()
                .forAttachmentKeyAndValue("desk", "blueberry")
                .inNearRange()
                .withOnEnterAction(makeBlueberryDeskFilled)
                .withOnExitAction(makeBlueberryDeskWhite)
                .create()

        // Add zones to ProximityObserver and START observation!
        observationHandler = proximityObserver
                .addProximityZones(venueZone, mintDeskZone, blueberryDeskZone)
                // Scan power mode - you can play with three different - low latency, low power, and balanced.
                // The default mode is balanced. If you have used our old SDK before (1.0.13 or so),
                // you might notice that we no longer allow to setup exact scan time periods.
                // This caused many misconceptions and from now on we will handle the proper scan setup for you.
                // This is cool, isn't it? Tell us what you think about it!
                .withBalancedPowerMode()
                // And now go ahead and launch the observation process!
                // Also, notice that we used here .startWithForegroundScanner method.
                // It takes your notification and will handle scanning in the foreground service,
                // so that the system won't kill your scanning as long as the user is playing with the app.
                .startWithScannerInForegroundService(notification)
                // You can also use .startWithSimpleScanner() which won't use any service for scan.
                // This will cause your scan to stop when user exits your app (or shortly after).
                // But you can use this method to implement some custom logic - maybe using your own service?
                // Read more about handling background scanning here:
                // https://github.com/Estimote/Android-Proximity-SDK#background-scanning
    }

    override fun onDestroy() {
        super.onDestroy()
        // After starting your scan, the Proximity Observer will return you a handler to stop the scanning process.
        // We will use it here to stop the scan when activity is destroyed.
        // IMPORTANT:
        // If you don't stop the scan here, the foreground service will remain active EVEN if the user kills your APP.
        // You can use it to retain scanning when app is killed, but you will need to handle actions properly.
        observationHandler.stop()
    }

}
