package torrent;

import com.google.common.hash.Hashing;
import debug.Debug;
import java.io.File;
import java.net.NetworkInterface;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.prefs.Preferences;
import listener.GuiListener;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.StringUtils;
import oshi.SystemInfo;
import str.Str;
import util.Connection;
import util.ConnectionException;
import util.Constant;
import util.IO;
import util.Regex;
import util.WindowsUtil;
import util.Worker;

public class StreamingTorrentLicense extends Worker {

  private final GuiListener guiListener;
  private final String activationCode;
  private static final String LICENSE_PREFERENCE = "5AB7006ABCEEB3050D02E3", SIGNED_LICENSE_PREFERENCE = "79DD778D5F9BBB53274AF2";

  public StreamingTorrentLicense(GuiListener guiListener, String activationCode) {
    this.guiListener = guiListener;
    this.activationCode = activationCode;
  }

  static boolean isExpired(GuiListener guiListener, boolean incrementTrialCount) {
    try {
      if (activationCode(guiListener, SIGNED_LICENSE_PREFERENCE, LICENSE_PREFERENCE) != null) {
        return false;
      }

      String[] expiredFlag = Regex.split(getSourceCode(incrementTrialCount ? 736 : 735, URLEncoder.encode(machineId(), Constant.UTF8) + '&' + URLEncoder.encode(
              Hashing.sha256().hashString(Constant.APP_DIR, StandardCharsets.UTF_8).toString().toUpperCase(Locale.ENGLISH), Constant.UTF8)), ",");
      return Boolean.parseBoolean(getSignedVal(expiredFlag[0], expiredFlag[1]));
    } catch (ConnectionException e) {
      if (Debug.DEBUG) {
        Debug.print(e);
      }
      return false;
    } catch (Throwable t) {
      if (Debug.DEBUG) {
        Debug.print(t);
      }
      return true;
    }
  }

  @Override
  protected void doWork() {
    guiListener.licenseActivationStarted();
    try {
      if (activationCode.isEmpty()) {
        buy();
        guiListener.msg(Str.str("activationBuy"), Constant.INFO_MSG);
      } else {
        activate(guiListener, activationCode);
      }
    } catch (Throwable t) {
      if (Debug.DEBUG) {
        Debug.print(t);
      }
      guiListener.msg(Str.str("pleaseRetry"), Constant.ERROR_MSG);
    }
    guiListener.licenseActivationStopped();
  }

  private static void buy() throws Exception {
    if (!Str.get(685).isEmpty()) {
      String activation = Constant.TEMP_DIR + "activation" + Constant.HTML;
      Connection.saveData(hexBinary(644) + Str.get(685).hashCode(), activation, null, false);
      if (Constant.WINDOWS) {
        WindowsUtil.addMicrosoftRegistryEntry("Internet Explorer\\Main\\FeatureControl\\FEATURE_LOCALMACHINE_LOCKDOWN", "DWORD", "iexplore.exe", "0");
      }
      IO.browse(new File(activation));
    }
  }

  private static void activate(GuiListener guiListener, String activationCode) throws Exception {
    String license = getSourceCode(645, URLEncoder.encode(machineId(), Constant.UTF8) + '&' + URLEncoder.encode(activationCode, Constant.UTF8));
    if (license.isEmpty()) {
      try {
        buy();
      } catch (Exception e) {
        if (Debug.DEBUG) {
          Debug.print(e);
        }
      }
      guiListener.licenseDeactivated();
      return;
    }

    String[] licenseParts = Regex.split(license, ",");
    setPreference(SIGNED_LICENSE_PREFERENCE, licenseParts[0]);
    setPreference(LICENSE_PREFERENCE, licenseParts[1]);
    if ((license = activationCode(guiListener, SIGNED_LICENSE_PREFERENCE, LICENSE_PREFERENCE)) == null) {
      try {
        buy();
      } catch (Exception e) {
        if (Debug.DEBUG) {
          Debug.print(e);
        }
      }
      guiListener.licenseDeactivated();
    } else {
      guiListener.licenseActivated(license);
    }
  }

  private static String activationCode(GuiListener guiListener, String signedLicensePreference, String licensePreference) {
    String licenseVal = getSignedVal(getPreference(signedLicensePreference), getPreference(licensePreference));
    if (Debug.DEBUG) {
      Debug.println("license:\n" + licenseVal);
    }
    String[] licenseParts;
    return licenseVal == null || (((licenseParts = Regex.split(licenseVal, Constant.STD_NEWLINE))[0].equals(Constant.NULL) || !licenseParts[0].equals(
            machineId())) && !licenseParts[1].equals(ip(guiListener))) ? null : licenseParts[2];
  }

  private static String ip(GuiListener guiListener) {
    try {
      return Magnet.getIp(guiListener);
    } catch (Throwable t) {
      if (Debug.DEBUG) {
        Debug.print(t);
      }
      return null;
    }
  }

  private static String getSourceCode(int urlIndex, String params) throws Exception {
    return Connection.getSourceCode(hexBinary(urlIndex) + params, null, false, true, -1).trim();
  }

  private static String machineId() {
    for (Callable<String> idGetter : Arrays.asList((Callable<String>) () -> (new SystemInfo()).getHardware().getComputerSystem().getHardwareUUID(),
            () -> (new SystemInfo()).getHardware().getComputerSystem().getBaseboard().getSerialNumber(),
            () -> {
              Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
              if (networkInterfaces != null) {
                List<String> macs = new ArrayList<String>(8);
                while (networkInterfaces.hasMoreElements()) {
                  byte[] mac = networkInterfaces.nextElement().getHardwareAddress();
                  if (mac != null && mac.length != 0) {
                    macs.add(Hex.encodeHexString(mac));
                  }
                }
                if (!macs.isEmpty()) {
                  Collections.sort(macs, Collections.reverseOrder());
                  return macs.get(0);
                }
              }
              return null;
            })) {
      try {
        String id = idGetter.call().trim();
        if (!StringUtils.equalsAnyIgnoreCase(id, "", "unknown", "none")) {
          return Hashing.sha256().hashString(id, StandardCharsets.UTF_8).toString().toUpperCase(Locale.ENGLISH);
        }
      } catch (Throwable t) {
        if (Debug.DEBUG) {
          Debug.print(t);
        }
      }
    }
    return Constant.NULL;
  }

  private static String getPreference(String preference) {
    Function<Function<Class<?>, Preferences>, String> getPreference = node -> {
      try {
        return node.apply(StreamingTorrentLicense.class).get(preference, null);
      } catch (Throwable t) {
        if (Debug.DEBUG) {
          Debug.print(t);
        }
        return null;
      }
    };
    String val = getPreference.apply(Preferences::userNodeForPackage);
    return val == null ? getPreference.apply(Preferences::systemNodeForPackage) : val;
  }

  private static void setPreference(String preference, String val) {
    Consumer<Function<Class<?>, Preferences>> setPreference = node -> {
      try {
        node.apply(StreamingTorrentLicense.class).put(preference, val);
      } catch (Throwable t) {
        if (Debug.DEBUG) {
          Debug.print(t);
        }
      }
    };
    setPreference.accept(Preferences::userNodeForPackage);
    setPreference.accept(Preferences::systemNodeForPackage);
  }

  private static String hexBinary(int index) throws Exception {
    return new String(Hex.decodeHex(Str.get(index)), Constant.UTF8);
  }

  private static String getSignedVal(String signedVal, String val) {
    try {
      Signature signature = Signature.getInstance("SHA1withDSA", "SUN");
      signature.initVerify(KeyFactory.getInstance("DSA", "SUN").generatePublic(new X509EncodedKeySpec(publicKey)));
      byte[] bytes = Hex.decodeHex(val);
      signature.update(bytes);
      return signature.verify(Hex.decodeHex(signedVal)) ? new String(bytes, Constant.UTF8) : null;
    } catch (Throwable t) {
      if (Debug.DEBUG) {
        Debug.println("Value '" + val + "' authenticity unverifiable: " + t.toString());
      }
      return null;
    }
  }

  private static final byte[] publicKey = {48, -126, 1, -72, 48, -126, 1, 44, 6, 7, 42, -122, 72, -50, 56, 4, 1, 48, -126, 1, 31, 2, -127, -127, 0, -3, 127, 83,
    -127, 29, 117, 18, 41, 82, -33, 74, -100, 46, -20, -28, -25, -10, 17, -73, 82, 60, -17, 68, 0, -61, 30, 63, -128, -74, 81, 38, 105, 69, 93, 64, 34, 81, -5,
    89, 61, -115, 88, -6, -65, -59, -11, -70, 48, -10, -53, -101, 85, 108, -41, -127, 59, -128, 29, 52, 111, -14, 102, 96, -73, 107, -103, 80, -91, -92, -97,
    -97, -24, 4, 123, 16, 34, -62, 79, -69, -87, -41, -2, -73, -58, 27, -8, 59, 87, -25, -58, -88, -90, 21, 15, 4, -5, -125, -10, -45, -59, 30, -61, 2, 53, 84,
    19, 90, 22, -111, 50, -10, 117, -13, -82, 43, 97, -41, 42, -17, -14, 34, 3, 25, -99, -47, 72, 1, -57, 2, 21, 0, -105, 96, 80, -113, 21, 35, 11, -52, -78,
    -110, -71, -126, -94, -21, -124, 11, -16, 88, 28, -11, 2, -127, -127, 0, -9, -31, -96, -123, -42, -101, 61, -34, -53, -68, -85, 92, 54, -72, 87, -71, 121,
    -108, -81, -69, -6, 58, -22, -126, -7, 87, 76, 11, 61, 7, -126, 103, 81, 89, 87, -114, -70, -44, 89, 79, -26, 113, 7, 16, -127, -128, -76, 73, 22, 113, 35,
    -24, 76, 40, 22, 19, -73, -49, 9, 50, -116, -56, -90, -31, 60, 22, 122, -117, 84, 124, -115, 40, -32, -93, -82, 30, 43, -77, -90, 117, -111, 110, -93, 127,
    11, -6, 33, 53, 98, -15, -5, 98, 122, 1, 36, 59, -52, -92, -15, -66, -88, 81, -112, -119, -88, -125, -33, -31, 90, -27, -97, 6, -110, -117, 102, 94, -128,
    123, 85, 37, 100, 1, 76, 59, -2, -49, 73, 42, 3, -127, -123, 0, 2, -127, -127, 0, -17, 30, -30, 19, -98, -116, 117, 21, -64, -40, 2, 112, -46, -113, 89,
    61, -19, -70, -74, -6, -61, -40, 25, 54, 40, 33, -28, -60, 57, -106, 17, -102, -47, -47, -38, -57, 106, 39, -94, 58, 44, 40, -78, 18, 7, -115, -78, 79,
    121, 113, -103, -85, 127, -82, 67, -14, 44, 42, -46, -42, -88, 93, 96, -74, 93, -24, -61, 45, 83, 107, -100, -82, 13, 86, -62, 97, -6, 0, -122, -93, -54,
    -3, -117, -80, -125, -21, 83, 123, -77, -99, -12, -58, -26, -23, -48, 42, 36, -56, 98, -28, 66, -2, 28, -70, 110, -49, -49, -36, 89, -104, -122, -2, -12,
    -5, -66, 83, -56, 79, 8, 38, 97, 57, 101, 125, -44, 55, 116, 82};
}
