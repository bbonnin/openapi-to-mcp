package io.openapimcp.example.api;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class VmRepository {

    private static final Map<String, VirtualMachine> VMS = new HashMap<>();

    static {
        VMS.put("web-01", new VirtualMachine("web-01", VmStatus.RUNNING, new VmMetrics(0.15f, 75)));
        VMS.put("db-01", new VirtualMachine("db-01", VmStatus.DOWN, new VmMetrics(0.30f, 45)));
        VMS.put("cache-01", new VirtualMachine("cache-01", VmStatus.STARTING, new VmMetrics(0.95f, 10)));
    }

    public static Collection<VirtualMachine> listAll() {
        return VMS.values();
    }

    public static VirtualMachine start(String name) {
        VirtualMachine vm = VMS.get(name);
        if (vm != null) {
            vm.status = VmStatus.STARTING;
        }
        return vm;
    }

    public static VirtualMachine addVm(String name) {
        VirtualMachine vm = new VirtualMachine(
                name,
                VmStatus.DOWN,
                new VmMetrics(0.0f, 0.0f));

        VMS.put(name, vm);
        return vm;
    }
}
