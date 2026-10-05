package com.orel.wallet.presentation.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.orel.wallet.domain.*
import com.orel.wallet.presentation.WalletViewModel
import com.orel.wallet.ui.components.*

@Composable fun AppearanceScreen(card:Card?,vm:WalletViewModel,onBack:()->Unit) {
    if(card==null) { Column { ScreenHeader("Card Appearance",onBack); EmptyState("Tarjeta no disponible","Vuelve al wallet") }; return }
    var draft by remember(card.id) { mutableStateOf(card.appearance) }
    var tab by remember { mutableIntStateOf(0) }
    var hex by remember { mutableStateOf("#0866F5") }
    val picker=rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> uri?.let { vm.importImage(it) {path -> draft=draft.copy(backgroundType=BackgroundType.IMAGE,backgroundValue=path)} } }
    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Card Appearance",onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal=24.dp)) {
            WalletCard(card.copy(appearance=draft))
            Text("La misma tarjeta. Tu propio estilo.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.padding(top=16.dp,bottom=18.dp))
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                listOf("Imagen","Color","Estilo","Texto").forEachIndexed { i,label -> FilterChip(tab==i,{tab=i},label={Text(label)}) }
            }
            when(tab) {
                0 -> {
                    SectionLabel("Custom Card Skin")
                    val skins=listOf("blue" to "Blue Silk","graphite" to "Carbon","gold" to "Gold Hour","violet" to "Aurora","mint" to "Lagoon","ice" to "Glacier")
                    skins.chunked(3).forEach { row ->
                        Row(Modifier.fillMaxWidth().padding(bottom=12.dp),horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                            row.forEach { (skin,name) -> Column(Modifier.weight(1f).clickable {draft=draft.copy(backgroundType=BackgroundType.SKIN,backgroundValue=skin)}.semantics {contentDescription="Skin $name"}) {
                                Box {
                                    Image(painterResource(skinResource(skin)),null,Modifier.fillMaxWidth().aspectRatio(1.35f).clip(RoundedCornerShape(14.dp)),contentScale=ContentScale.Crop)
                                    if(draft.backgroundType==BackgroundType.SKIN && draft.backgroundValue==skin) Icon(Icons.Outlined.CheckCircle,"Seleccionado",tint=Color.White,modifier=Modifier.align(Alignment.BottomEnd).padding(6.dp).size(20.dp))
                                }
                                Text(name,style=MaterialTheme.typography.bodySmall,modifier=Modifier.padding(top=6.dp),color=MaterialTheme.colorScheme.onSurfaceVariant)
                            } }
                        }
                    }
                    OutlinedButton({picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))},Modifier.fillMaxWidth()) { Icon(Icons.Outlined.AddPhotoAlternate,null); Spacer(Modifier.width(8.dp)); Text("Elegir imagen de la galería") }
                    Text("La imagen se guarda únicamente en este dispositivo.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.padding(top=8.dp))
                }
                1 -> {
                    SectionLabel("Color de fondo")
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                        listOf("#0866F5","#202938","#926B37","#7457D8","#08796F","#C6DAED").forEach { color ->
                            ColorSwatch(Color(android.graphics.Color.parseColor(color)),"Fondo $color",draft.backgroundValue==color) {hex=color; draft=draft.copy(backgroundType=BackgroundType.COLOR,backgroundValue=color)}
                        }
                    }
                    OutlinedTextField(hex,{value -> hex=value.take(7); if(value.matches(Regex("#[0-9a-fA-F]{6}"))) draft=draft.copy(backgroundType=BackgroundType.COLOR,backgroundValue=value)},label={Text("Color HEX")},singleLine=true,modifier=Modifier.fillMaxWidth().padding(top=16.dp))
                    SettingRow(Icons.Outlined.Gradient,"Aplicar gradiente","Añadir profundidad al color",trailing={WalletSwitch("Aplicar gradiente",draft.backgroundType==BackgroundType.GRADIENT,{draft=draft.copy(backgroundType=if(it) BackgroundType.GRADIENT else BackgroundType.COLOR,backgroundValue=hex.takeIf {v->v.matches(Regex("#[0-9a-fA-F]{6}"))} ?: "#0866F5")})})
                }
                2 -> {
                    SectionLabel("Chip visual")
                    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) { ChipStyle.entries.forEach { type -> FilterChip(draft.chipStyle==type,{draft=draft.copy(chipStyle=type)},label={Text(when(type){ChipStyle.SILVER->"Plata"; ChipStyle.GOLD->"Oro"; ChipStyle.MINIMAL->"Minimal"})}) } }
                    SectionLabel("Posición del número")
                    Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) { NumberPosition.entries.forEach { type -> FilterChip(draft.numberPosition==type,{draft=draft.copy(numberPosition=type)},label={Text(if(type==NumberPosition.TOP) "Arriba" else "Abajo")}) } }
                }
                3 -> {
                    SectionLabel("Tipografía")
                    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) { TextStyle.entries.forEach { type -> FilterChip(draft.textStyle==type,{draft=draft.copy(textStyle=type)},label={Text(if(type==TextStyle.CLASSIC) "Clásica" else "Monoespaciada")}) } }
                    Row(Modifier.padding(vertical=16.dp),horizontalArrangement=Arrangement.spacedBy(14.dp)) {
                        ColorSwatch(Color.White,"Texto blanco",draft.textColor==0xFFFFFFFF) {draft=draft.copy(textColor=0xFFFFFFFF)}
                        ColorSwatch(Color(0xFF111C30),"Texto oscuro",draft.textColor==0xFF111C30) {draft=draft.copy(textColor=0xFF111C30)}
                    }
                    SettingRow(Icons.Outlined.Badge,"Mostrar nombre",trailing={WalletSwitch("Mostrar nombre en tarjeta",draft.showName,{draft=draft.copy(showName=it)})})
                }
            }
            SectionLabel("Ajustes de imagen")
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) { Text("Brillo",style=MaterialTheme.typography.bodyMedium); Text("${(draft.brightness*100).toInt()}%",style=MaterialTheme.typography.bodySmall) }
            Slider(draft.brightness,{draft=draft.copy(brightness=it)},modifier=Modifier.semantics {contentDescription="Brillo de la tarjeta"})
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) { Text("Contraste",style=MaterialTheme.typography.bodyMedium); Text("${(draft.contrast*100).toInt()}%",style=MaterialTheme.typography.bodySmall) }
            Slider(draft.contrast,{draft=draft.copy(contrast=it)},modifier=Modifier.semantics {contentDescription="Contraste de la tarjeta"})
            Text("La apariencia no modifica la red, el emisor ni las credenciales de pago.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.padding(bottom=24.dp))
        }
        Surface(shadowElevation=4.dp) { Button({vm.appearance(card.id,draft,onBack)},Modifier.fillMaxWidth().padding(horizontal=24.dp,vertical=16.dp).height(52.dp)) { Text("Guardar apariencia") } }
    }
}

@Composable fun ColorSwatch(color:Color,label:String,selected:Boolean,onClick:()->Unit) {
    Box(Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.outlineVariant).clickable(onClick=onClick).semantics {contentDescription=label}.padding(3.dp).clip(CircleShape).background(color),contentAlignment=Alignment.Center) {
        if(selected) Icon(Icons.Outlined.Check,null,tint=if(color==Color.White) Color.Black else Color.White,modifier=Modifier.size(18.dp))
    }
}
