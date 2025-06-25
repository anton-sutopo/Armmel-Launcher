
package armmel.contacts.orm;

import java.lang.annotation.*;


    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.PARAMETER)
    public @interface Param {
        String value();
    }
