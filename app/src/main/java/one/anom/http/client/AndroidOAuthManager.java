package one.anom.http.client;

import com.samourai.wallet.util.oauth.OAuthApi;
import com.samourai.wallet.util.oauth.OAuthManager;

import one.anom.wallet.api.APIFactory;

import org.apache.commons.lang3.StringUtils;

public class AndroidOAuthManager implements OAuthManager {
    private APIFactory apiFactory;

    public AndroidOAuthManager(APIFactory apiFactory) {
        this.apiFactory = apiFactory;
    }

    @Override
    public String getOAuthAccessToken(OAuthApi oAuthApi) throws Exception {
        String accessToken = apiFactory.getAccessTokenNotExpired();
        if (StringUtils.isEmpty(accessToken)) {
            throw new Exception("AccessToken not available");
        }
        return accessToken;
    }

}