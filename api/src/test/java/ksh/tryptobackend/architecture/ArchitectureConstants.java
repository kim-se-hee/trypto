package ksh.tryptobackend.architecture;

import java.util.Arrays;

public final class ArchitectureConstants {

    private ArchitectureConstants() {}

    static final String BASE = "ksh.tryptobackend";

    static final String[] BOUNDED_CONTEXTS = {
        "trading",
        "wallet",
        "transfer",
        "investmentround",
        "ranking",
        "regretanalysis",
        "portfolio",
        "marketdata",
        "user"
    };

    static final String COMMON = BASE + ".common";
    static final String BATCH = BASE + ".batch";

    static final String DOMAIN = ".domain..";
    static final String APPLICATION = ".application..";
    static final String ADAPTER = ".adapter..";
    static final String ADAPTER_IN = ".adapter.in..";
    static final String ADAPTER_OUT = ".adapter.out..";
    static final String SERVICE = ".application.service..";
    static final String DOMAIN_SERVICE = ".domain.service..";
    static final String PORT_IN = ".application.port.in";
    static final String PORT_OUT = ".application.port.out";
    static final String STRATEGY = ".application.strategy..";

    static String contextPkg(String context, String layer) {
        return BASE + "." + context + layer;
    }

    static String[] allContextPackages(String layer) {
        return Arrays.stream(BOUNDED_CONTEXTS)
                .map(ctx -> contextPkg(ctx, layer))
                .toArray(String[]::new);
    }

    static String[] allContextDirectPackages(String layer) {
        return Arrays.stream(BOUNDED_CONTEXTS)
                .map(ctx -> BASE + "." + ctx + layer)
                .toArray(String[]::new);
    }

    static String[] forbiddenPackagesOf(String context) {
        return new String[] {
            contextPkg(context, DOMAIN),
            contextPkg(context, ADAPTER),
            contextPkg(context, SERVICE),
            contextPkg(context, ".application.port.out.."),
            contextPkg(context, STRATEGY)
        };
    }

    static String[] allContextForbiddenPackages() {
        return Arrays.stream(BOUNDED_CONTEXTS)
                .flatMap(ctx -> Arrays.stream(forbiddenPackagesOf(ctx)))
                .toArray(String[]::new);
    }

    static String[] allContextRootPackages() {
        return Arrays.stream(BOUNDED_CONTEXTS)
                .map(ctx -> BASE + "." + ctx + "..")
                .toArray(String[]::new);
    }

    static String[] merge(String[]... arrays) {
        int total = 0;
        for (String[] arr : arrays) total += arr.length;
        String[] result = new String[total];
        int pos = 0;
        for (String[] arr : arrays) {
            System.arraycopy(arr, 0, result, pos, arr.length);
            pos += arr.length;
        }
        return result;
    }

    static String[] otherContextPortInPackages(String context) {
        return Arrays.stream(BOUNDED_CONTEXTS)
                .filter(ctx -> !ctx.equals(context))
                .map(ctx -> contextPkg(ctx, ".application.port.in.."))
                .toArray(String[]::new);
    }

    static String[] otherContextPortOutPackages(String context) {
        return Arrays.stream(BOUNDED_CONTEXTS)
                .filter(ctx -> !ctx.equals(context))
                .map(ctx -> contextPkg(ctx, ".application.port.out.."))
                .toArray(String[]::new);
    }
}
