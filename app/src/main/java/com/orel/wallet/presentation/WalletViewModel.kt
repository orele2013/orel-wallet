package com.orel.wallet.presentation

import android.content.Context
import android.app.Application
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orel.wallet.domain.*
import com.orel.wallet.nfc.NfcController
import com.orel.wallet.nfc.NfcStatus
import com.orel.wallet.payments.DemoPaymentService
import com.orel.wallet.payments.PaymentSession
import com.orel.wallet.payments.PaymentService
import com.orel.wallet.BuildConfig
import com.orel.wallet.wallet.WalletRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File
import java.util.UUID

data class WalletUiState(
    val cards:List<Card> = emptyList(),val transactions:List<Transaction> = emptyList(),
    val settings:WalletSettings = WalletSettings(),val loaded:Boolean=false
)

class WalletViewModel(val repository:WalletRepository,val paymentService:PaymentService,context:Context):ViewModel() {
    private val appContext=context.applicationContext as Application
    private val ready=MutableStateFlow(false)
    val ui:StateFlow<WalletUiState> = combine(repository.cards,repository.transactions,repository.settings,ready) { cards,tx,settings,loaded -> WalletUiState(cards,tx,settings,loaded) }
        .stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),WalletUiState())
    val selectedId=MutableStateFlow<String?>(null)
    val message=MutableStateFlow<String?>(null)
    val nfc=MutableStateFlow(NfcStatus(false,false,false))
    val walletLocked=MutableStateFlow(true)
    private var backgroundAt=0L
    fun unlockWallet() {walletLocked.value=false}
    fun background() {backgroundAt=android.os.SystemClock.elapsedRealtime()}
    fun foreground(settings:WalletSettings) {
        if(backgroundAt>0L && settings.requireBiometric && android.os.SystemClock.elapsedRealtime()-backgroundAt>=settings.lockTimeoutSeconds*1000L) walletLocked.value=true
        backgroundAt=0
        refreshNfc()
    }
    init { perform { repository.initialize(); ready.value=true; refreshNfc() } }
    fun refreshNfc() { nfc.value=NfcController(appContext).status() }
    fun select(id:String) { selectedId.value=id }
    fun clearMessage() { message.value=null }
    private fun perform(action:suspend ()->Unit) { viewModelScope.launch { try { action() } catch(e:CancellationException) { throw e } catch(e:Exception) { message.value=e.message ?: "No se pudo completar la operación" } } }
    fun add(network:CardNetwork,name:String,onAdded:(String)->Unit) = perform { check(BuildConfig.DEMO_MODE); val card=repository.addCard(network,name); selectedId.value=card.id; onAdded(card.id) }
    fun addExternal(network:CardNetwork,name:String,last4:String,onAdded:(String)->Unit,onError:()->Unit) {
        viewModelScope.launch {
            try {val card=repository.addExternalCard(network,name,last4); selectedId.value=card.id; onAdded(card.id)}
            catch(e:CancellationException) {throw e}
            catch(e:Exception) {message.value=e.message ?: "No se pudo guardar la referencia"; onError()}
        }
    }
    fun update(card:Card) = perform { repository.updateCard(card) }
    fun remove(card:Card,onRemoved:()->Unit) = perform { repository.removeCard(card.id); if(selectedId.value==card.id) selectedId.value=null; onRemoved() }
    fun default(card:Card) = perform { repository.setDefault(card.id) }
    fun appearance(cardId:String,appearance:CardAppearance,onSaved:()->Unit) = perform { repository.saveAppearance(cardId,appearance); message.value="Apariencia guardada"; onSaved() }
    fun settings(value:WalletSettings) = perform { repository.saveSettings(value) }
    fun move(cardId:String,direction:Int) = perform {
        val ids=repository.cards.first().sortedBy { it.sortOrder }.map { it.id }.toMutableList()
        val from=ids.indexOf(cardId); val to=from+direction
        if(from>=0 && to in ids.indices) { java.util.Collections.swap(ids,from,to); repository.reorder(ids) }
    }
    fun importImage(uri:Uri,onImported:(String)->Unit) = perform {
        val path=withContext(Dispatchers.IO) {
            val mime=appContext.contentResolver.getType(uri)
            require(mime?.startsWith("image/")==true) { "Selecciona una imagen válida" }
            val dir=File(appContext.noBackupFilesDir,"card-images").apply { mkdirs() }
            val file=File(dir,"${UUID.randomUUID()}.image")
            try {
                appContext.contentResolver.openInputStream(uri)?.use { input -> file.outputStream().use { out ->
                    val buffer=ByteArray(8192); var total=0; var count=input.read(buffer)
                    while(count!=-1) { total+=count; require(total<=12*1024*1024) { "La imagen debe ocupar menos de 12 MB" }; out.write(buffer,0,count); count=input.read(buffer) }
                } } ?: error("No se pudo leer la imagen")
                file.absolutePath
            } catch(e:Exception) { file.delete(); throw e }
        }
        onImported(path)
    }
    fun beginPayment(cardId:String,onPrepared:(String)->Unit,onError:(String)->Unit) {
        viewModelScope.launch {
            try {onPrepared(paymentService.preparePayment(cardId).id)}
            catch(e:CancellationException) {throw e}
            catch(e:Exception) {onError(e.message ?: "No se pudo preparar la demo")}
        }
    }
    fun demoAuthenticate(session:PaymentSession) = perform { check(BuildConfig.DEMO_MODE); (paymentService as DemoPaymentService).authenticateDemo(session) }
    fun authenticated(session:PaymentSession) = perform { paymentService.authenticate(session) }
    fun executeDemo(session:PaymentSession) = perform { paymentService.executePayment(session) }
    fun cancelPayment() { paymentService.cancel() }
}
