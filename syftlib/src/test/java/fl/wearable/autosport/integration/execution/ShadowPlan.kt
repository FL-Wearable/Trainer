package fl.wearable.autosport.integration.execution

import fl.wearable.autosport.execution.Plan
import org.pytorch.IValue
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements

@ExperimentalUnsignedTypes
@Implements(Plan::class)
class ShadowPlan {

    @Implementation
    fun execute(vararg iValues: IValue): IValue? {
        return null
    }

    @Implementation
    fun loadScriptModule(torchScriptLocation: String) = Unit
}