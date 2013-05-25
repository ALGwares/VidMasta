package torrent;

import debug.Debug;
import gnu.trove.list.array.TIntArrayList;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.gudy.azureus2.core3.tracker.protocol.PRHelpers;
import util.Constant;
import util.io.CleanUp;
import util.io.Write;

class IpInitializer extends Thread {

    TIntArrayList ips = new TIntArrayList(0), ipRanges = new TIntArrayList(0);
    private static final String IP_REGEX = "\\d{1,3}+\\.\\d{1,3}+\\.\\d{1,3}+\\.\\d{1,3}+";
    private final Pattern ipPattern = Pattern.compile(IP_REGEX);
    private final Pattern ipRangePattern = Pattern.compile(IP_REGEX + "\\s*+-\\s*+" + IP_REGEX);

    @Override
    public void run() {
        try {
            initIPs();
        } catch (Exception e) {
            if (Debug.DEBUG) {
                Debug.print(e);
            }
        }
    }

    private void initIPs() throws Exception {
        if (!(new File(Constant.APP_DIR + Constant.IP_FILTER)).exists() || !(new File(Constant.APP_DIR + "ipfilter" + Constant.APP_VERSION)).exists()) {
            Write.unzip(Constant.PROGRAM_DIR + Constant.IP_FILTER + Constant.ZIP, Constant.APP_DIR);
            Write.write(Constant.APP_DIR + "ipfilter" + Constant.APP_VERSION, "");
        }

        final Runtime runtime = Runtime.getRuntime();
        final BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(Constant.APP_DIR + Constant.IP_FILTER), Constant.UTF8));

        ips = new TIntArrayList(500000);
        ipRanges = new TIntArrayList(1500000);

        try {
            String line, range;
            while ((line = br.readLine()) != null) {
                if (runtime.freeMemory() < 32768L || line.length() > 256) {
                    break;
                }
                if ((range = getIp(line, ipRangePattern)) == null) {
                    if ((range = getIp(line, ipPattern)) != null) {
                        try {
                            ips.add(PRHelpers.addressToInt(range.trim()));
                        } catch (Exception e) {
                            if (Debug.DEBUG) {
                                Debug.print(e);
                            }
                        }
                    }
                } else {
                    final String[] ipRangeParts = range.split("-");
                    try {
                        final int startIp = PRHelpers.addressToInt(ipRangeParts[0].trim());
                        final int endIp = PRHelpers.addressToInt(ipRangeParts[1].trim());
                        ipRanges.add(startIp);
                        ipRanges.add(endIp);
                    } catch (Exception e) {
                        if (Debug.DEBUG) {
                            Debug.print(e);
                        }
                    }
                }
            }
        } finally {
            ips.trimToSize();
            ipRanges.trimToSize();
            CleanUp.close(br);
        }
    }

    private static String getIp(final String input, final Pattern pattern) {
        final Matcher matcher = pattern.matcher(input);
        while (!matcher.hitEnd()) {
            if (matcher.find()) {
                return matcher.group();
            }
        }
        return null;
    }
}
