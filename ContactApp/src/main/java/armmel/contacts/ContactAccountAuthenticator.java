package armmel.contacts;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.AccountManager;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.NetworkErrorException;
import android.content.Context;
import android.os.Bundle;

public class ContactAccountAuthenticator extends AbstractAccountAuthenticator {
  public ContactAccountAuthenticator(Context context) {
    super(context);
  }

  @Override
  public Bundle editProperties(AccountAuthenticatorResponse response, String accountType) {
    return unsupported();
  }

  @Override
  public Bundle addAccount(
      AccountAuthenticatorResponse response,
      String accountType,
      String authTokenType,
      String[] requiredFeatures,
      Bundle options)
      throws NetworkErrorException {
    return unsupported();
  }

  @Override
  public Bundle confirmCredentials(
      AccountAuthenticatorResponse response, android.accounts.Account account, Bundle options)
      throws NetworkErrorException {
    return unsupported();
  }

  @Override
  public Bundle getAuthToken(
      AccountAuthenticatorResponse response,
      android.accounts.Account account,
      String authTokenType,
      Bundle options)
      throws NetworkErrorException {
    return unsupported();
  }

  @Override
  public String getAuthTokenLabel(String authTokenType) {
    return null;
  }

  @Override
  public Bundle updateCredentials(
      AccountAuthenticatorResponse response,
      android.accounts.Account account,
      String authTokenType,
      Bundle options)
      throws NetworkErrorException {
    return unsupported();
  }

  @Override
  public Bundle hasFeatures(
      AccountAuthenticatorResponse response,
      android.accounts.Account account,
      String[] features)
      throws NetworkErrorException {
    Bundle result = new Bundle();
    result.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, false);
    return result;
  }

  private Bundle unsupported() {
    Bundle result = new Bundle();
    result.putInt(
        AccountManager.KEY_ERROR_CODE, AccountManager.ERROR_CODE_UNSUPPORTED_OPERATION);
    return result;
  }
}
