package com.ganyo.pushtest;

import android.content.Context;

import android.util.Log;

import com.apigee.sdk.ApigeeClient;
import com.apigee.sdk.data.client.DataClient;
import com.apigee.sdk.data.client.callbacks.ApiResponseCallback;
import com.apigee.sdk.data.client.callbacks.DeviceRegistrationCallback;
import com.apigee.sdk.data.client.entities.Device;
import com.apigee.sdk.data.client.entities.Entity;
import com.apigee.sdk.data.client.response.ApiResponse;
import com.apigee.sdk.data.client.utils.JsonUtils;

import java.util.HashMap;

import static com.ganyo.pushtest.Util.*;
import static com.ganyo.pushtest.Settings.*;

public final class AppServices {

  private static DataClient client;
  private static Device device;

  static synchronized DataClient getClient(Context context) {
    if (client == null) {
      ApigeeClient apigeeClient = new ApigeeClient(ORG,APP,API_URL,context);
      client = apigeeClient.getDataClient();
    }
    return client;
  }

  static void login(final Context context) {

    if (USER != null) {
      getClient(context).authorizeAppUserAsync(USER, PASSWORD, new ApiResponseCallback() {

        @Override
        public void onResponse(ApiResponse apiResponse) {
          Log.i(TAG, "login response: " + apiResponse);
          registerPush(context);
        }

        @Override
        public void onException(Exception e) {
          displayMessage(context, "Login Exception: " + e);
          Log.i(TAG, "login exception: " + e);
        }
      });
    } else {
      registerPush(context);
    }
  }

  /**
   * Register this user/device pair on App Services.
   */
  static void register(final Context context, final String regId) {
    Log.i(TAG, "registering device: " + regId);

    getClient(context).registerDeviceForPushAsync(context, NOTIFIER, regId, null, new DeviceRegistrationCallback() {

      @Override
      public void onResponse(Device device) {
//        Log.i(TAG, "register response: " + device);
        AppServices.device = device;
        displayMessage(context, "Device registered as: " + regId);
        DataClient dataClient = getClient(context);

        // connect Device to current User - if there is one
        if (dataClient.getLoggedInUser() != null) {
          dataClient.connectEntitiesAsync("users", dataClient.getLoggedInUser().getUuid().toString(),
              "devices", device.getUuid().toString(),
              new ApiResponseCallback() {
                @Override
                public void onResponse(ApiResponse apiResponse) {
                  Log.i(TAG, "connect response: " + apiResponse);
                }

                @Override
                public void onException(Exception e) {
                  displayMessage(context, "Connect Exception: " + e);
                  Log.i(TAG, "connect exception: " + e);
                }
              });
        }
      }

      @Override
      public void onException(Exception e) {
        displayMessage(context, "Register Exception: " + e);
        Log.i(TAG, "register exception: " + e);
      }

      @Override
      public void onDeviceRegistration(Device device) { /* this won't ever be called */ }
    });
  }

  static void sendMyselfANotification(final Context context) {
    if (device == null) {
      displayMessage(context, "Device not registered (yet?)");
    }
    else {
      DataClient dataClient = getClient(context);
      String entityPath = "devices/" + device.getUuid().toString() + "/notifications";
      Entity notification = new Entity(dataClient,entityPath);

      HashMap<String,String> payloads = new HashMap<String, String>();
      payloads.put("google", "Hi there!");
      notification.setProperty("payloads", JsonUtils.toJsonNode(payloads));
      dataClient.createEntityAsync(notification, new ApiResponseCallback() {

        @Override
        public void onResponse(ApiResponse apiResponse) {
          Log.i(TAG, "send response: " + apiResponse);
        }

        @Override
        public void onException(Exception e) {
          displayMessage(context, "Send Exception: " + e);
          Log.i(TAG, "send exception: " + e);
        }
      });
    }
  }

  /**
   * Unregister this device within the server.
   */
  static void unregister(final Context context, final String regId) {
    Log.i(TAG, "unregistering device: " + regId);
    register(context, "");
  }
}
