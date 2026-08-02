package armmel.contacts;

import android.accounts.AbstractAccountAuthenticator;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class ContactAccountAuthenticatorService extends Service {
  private ContactAccountAuthenticator authenticator;

  @Override
  public void onCreate() {
    super.onCreate();
    authenticator = new ContactAccountAuthenticator(this);
  }

  @Override
  public IBinder onBind(Intent intent) {
    return authenticator.getIBinder();
  }
}
