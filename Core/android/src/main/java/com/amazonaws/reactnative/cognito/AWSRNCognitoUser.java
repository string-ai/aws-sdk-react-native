package com.amazonaws.reactnative.cognito;

import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoDevice;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUser;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserDetails;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserPool;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserSession;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.AuthenticationContinuation;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.AuthenticationDetails;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.ChallengeContinuation;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.MultiFactorAuthenticationContinuation;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.AuthenticationHandler;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.GetDetailsHandler;
import com.amazonaws.reactnative.core.AWSRNCognitoCredentials;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;


public class AWSRNCognitoUser extends ReactContextBaseJavaModule {

    public static final String CLIENT_ID = "clientId";
    public static final String USER_POOL_ID = "userPoolId";
    public static final String CLIENT_SECRET = "clientSecret";

    private static final SimpleDateFormat DATE_FORMATER = new SimpleDateFormat("EE MMM d y H:m:s ZZZ");
    public static final String STATUS = "status";
    public static final String ACCESS_TOKEN = "accessToken";
    public static final String OK_VALUE = "ok";
    public static final String ID_TOKEN = "idToken";
    public static final String REFRESH_TOKEN = "refreshToken";
    public static final String ATTRIBUTES = "attributes";
    public static final String SETTINGS = "settings";

    private final AWSRNCognitoCredentials awsrnCognitoCredentials;

    //private String userName;
    private String userPoolId;
    private String clientId;
    private String clientSecret;
    private CognitoUserPool userPool;
    private CognitoUserSession userSession;
    private CognitoDevice newDevice;
    private AuthenticationDetails authDetails;
    private CognitoUser user;
    private Promise authPromise;

    @Override
    public String getName() {
        return "AWSRNCognitoUser";
    }

    public AWSRNCognitoUser(ReactApplicationContext reactContext, AWSRNCognitoCredentials awsrnCognitoCredentials) {
        super(reactContext);
        this.awsrnCognitoCredentials = awsrnCognitoCredentials;
    }

    @ReactMethod
    public void initWithOptions(final ReadableMap options) throws IllegalArgumentException {
        if (!options.hasKey(CLIENT_SECRET) || !options.hasKey(CLIENT_ID) || !options.hasKey(USER_POOL_ID)) {
            throw new IllegalArgumentException("clientSecret, clientId and userPoolId are required");
        } else {
            this.userPoolId = options.getString(USER_POOL_ID);
            this.clientId = options.getString(CLIENT_ID);
            this.clientSecret = options.getString(CLIENT_SECRET);

            this.userPool = new CognitoUserPool(
                    this.getReactApplicationContext().getApplicationContext(),
                    userPoolId,
                    clientId,
                    clientSecret);

        }
    }

    private void storeUserSession(CognitoUserSession userSession) {
        this.userSession = userSession;
    }

    private void storeCognitoDevice(CognitoDevice newDevice) {
        this.newDevice = newDevice;
    }

    // Implement authentication handler,
    AuthenticationHandler handler = new AuthenticationHandler() {

        @Override
        public void onSuccess(CognitoUserSession userSession, CognitoDevice newDevice) {
            storeUserSession(userSession);
            storeCognitoDevice(newDevice);

            WritableMap result = Arguments.createMap();
            result.putString(STATUS, OK_VALUE);
            result.putMap(ACCESS_TOKEN, parseAccessToken(userSession));
            result.putMap(ID_TOKEN, parseIdToken(userSession));
            result.putMap(REFRESH_TOKEN, parseRefreshToken(userSession));

            authPromise.resolve(result);
        }

        @Override
        public void getAuthenticationDetails(final AuthenticationContinuation continuation, final String userID) {
            // User authentication details, userId and password are required to continue.
            // Use the "continuation" object to pass the user authentication details

            // After the user authentication details are available, wrap them in an AuthenticationDetails class
            // Along with userId and password, parameters for user pools for Lambda can be passed here
            // The validation parameters "validationParameters" are passed in as a Map<String, String>


            // Now allow the authentication to continue
            continuation.setAuthenticationDetails(authDetails);
            continuation.continueTask();
        }

        @Override
        public void getMFACode(final MultiFactorAuthenticationContinuation continuation) {
            // Multi-factor authentication is required to authenticate
            // A code was sent to the user, use the code to continue with the authentication


            // Find where the code was sent to
            //String codeSentHere = continuation.getParameter()[0];

            // When the verification code is available, continue to authenticate
            //continuation.setMfaCode(code);
            //continuation.continueTask();
        }

        @Override
        public void authenticationChallenge(final ChallengeContinuation continuation) {
            // A custom challenge has to be solved to authenticate

            // Set the challenge responses

            // Call continueTask() method to respond to the challenge and continue with authentication.
        }

        @Override
        public void onFailure(final Exception exception) {
            exception.printStackTrace();
            authPromise.reject("1", "Failed on authentication", exception);
        }
    };


    private WritableMap parseRefreshToken(CognitoUserSession userSession) {
        WritableMap result = Arguments.createMap();
        result.putString("token", userSession.getRefreshToken().getToken());
        return result;
    }

    private WritableMap parseIdToken(CognitoUserSession userSession) {
        WritableMap result = Arguments.createMap();
        result.putString("jwtToken", userSession.getIdToken().getJWTToken());
        result.putString("expiration", DATE_FORMATER.format(userSession.getIdToken().getExpiration()));
        result.putString("issuedAt", DATE_FORMATER.format(userSession.getIdToken().getIssuedAt()));
        //result.putString("notBefore", DATE_FORMATER.format(userSession.getIdToken().getNotBefore()));
        return result;
    }

    private WritableMap parseAccessToken(CognitoUserSession userSession) {
        WritableMap accessToken = Arguments.createMap();
        accessToken.putString("jwtToken", userSession.getAccessToken().getJWTToken());
        String formatedDate = DATE_FORMATER.format(userSession.getAccessToken().getExpiration());
        accessToken.putString("expiration", formatedDate);
        return accessToken;
    }


    @ReactMethod
    public void authenticateUser(final String userName, final String password, final Promise promise) {
        if (userName == null || password == null) {
            throw new IllegalArgumentException("userName and password are required");
        } else {

            //TODO: handle validationParameters
            //options.getMap("validationParameters")

            if(userPool.getCurrentUser() != null) {
                userPool.getCurrentUser().signOut();
            }

            this.authDetails = new AuthenticationDetails(userName, password, new HashMap<String, String>());
            this.user = userPool.getUser(userName);

            this.authPromise = promise;
            this.user.getSession(handler);
        }
    }

    @ReactMethod
    public void signOut(final Promise promise) {
        if(userPool.getCurrentUser() != null) {
            userPool.getCurrentUser().signOut();
            WritableMap result = Arguments.createMap();
            result.putString(STATUS, OK_VALUE);
            promise.resolve(result);
            return;
        }
        promise.reject("invalid_session", "No user logged in.");
    }

    @ReactMethod
    public void getUserAttributes(final Promise promise) {
        if(this.user == null) {
            promise.reject("no_auth", "User not authenticated");
            return;
        }
        this.user.getDetails(new GetDetailsHandler() {
            @Override
            public void onSuccess(CognitoUserDetails cognitoUserDetails) {
                Map<String, String> attributes = cognitoUserDetails.getAttributes().getAttributes();
                Map<String, String> settings = cognitoUserDetails.getSettings().getSettings();
                WritableMap attributesMap = Arguments.createMap();
                for(Map.Entry<String, String> entry: attributes.entrySet()) {
                    attributesMap.putString(entry.getKey(), entry.getValue());
                }
                WritableMap settingsMap = Arguments.createMap();
                for(Map.Entry<String, String> entry: settings.entrySet()) {
                    settingsMap.putString(entry.getKey(), entry.getValue());
                }
                WritableMap result = Arguments.createMap();
                result.putString(STATUS, OK_VALUE);
                result.putMap(ATTRIBUTES, attributesMap);
                result.putMap(SETTINGS, settingsMap);

                promise.resolve(result);
            }

            @Override
            public void onFailure(Exception e) {
                promise.reject("Unknown", "Unknown error: ", e);
            }
        });
    }

}
