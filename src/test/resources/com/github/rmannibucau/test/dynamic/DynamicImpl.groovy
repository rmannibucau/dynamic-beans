import com.github.rmannibucau.test.dynamic.DynamicBeanTest

class DynamicImpl implements DynamicBeanTest.Dynamic {
    @Override
    String value() {
        "dynamic"
    }
}
