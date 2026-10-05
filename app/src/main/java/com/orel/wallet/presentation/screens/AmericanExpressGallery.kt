package com.orel.wallet.presentation.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.orel.wallet.domain.*
import com.orel.wallet.ui.components.cardDesignResource

@Composable
internal fun AmericanExpressGallery(appearance: CardAppearance, onSelect: (CardDesign) -> Unit) {
    var query by remember { mutableStateOf("") }
    var group by remember { mutableStateOf<CardDesignGroup?>(null) }
    val designs = remember(group, query) { CardDesignCatalog.matching(group, query) }
    Text("American Express", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top=18.dp))
    Text("${CardDesignCatalog.designs.size} diseños de catálogo · colección internacional",
        style=MaterialTheme.typography.bodySmall, color=MaterialTheme.colorScheme.onSurfaceVariant,
        modifier=Modifier.padding(top=4.dp,bottom=12.dp))
    OutlinedTextField(query, {query=it.take(80)}, label={Text("Buscar diseño o país")}, singleLine=true,
        leadingIcon={Icon(Icons.Outlined.Search,null)},
        trailingIcon={if(query.isNotEmpty()) IconButton({query=""}) {Icon(Icons.Outlined.Close,"Borrar búsqueda")}},
        modifier=Modifier.fillMaxWidth())
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical=8.dp),
        horizontalArrangement=Arrangement.spacedBy(8.dp)) {
        FilterChip(group==null,{group=null},label={Text("Todas")})
        CardDesignGroup.entries.forEach { category ->
            FilterChip(group==category,{group=category},label={Text(category.label)})
        }
    }
    Text("${designs.size} ${if(designs.size==1) "diseño" else "diseños"}",
        style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant,
        modifier=Modifier.padding(bottom=12.dp))
    if(designs.isEmpty()) {
        Text("No hay diseños con esa búsqueda. Prueba otro modelo o país.",
            style=MaterialTheme.typography.bodyMedium,modifier=Modifier.padding(vertical=24.dp))
    }
    designs.chunked(2).forEach { row ->
        Row(Modifier.fillMaxWidth().padding(bottom=12.dp),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            row.forEach { design ->
                val selected=CardDesignCatalog.find(appearance)?.id==design.id
                Surface(Modifier.weight(1f).selectable(selected=selected,role=Role.RadioButton,onClick={onSelect(design)})
                    .semantics {contentDescription="Diseño ${design.name}, ${design.region}"},
                    shape=RoundedCornerShape(16.dp),
                    color=if(selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
                    border=androidx.compose.foundation.BorderStroke(if(selected) 2.dp else 1.dp,
                        if(selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)) {
                    Column(Modifier.padding(8.dp)) {
                        Box {
                            Image(painterResource(cardDesignResource(design)),null,
                                Modifier.fillMaxWidth().aspectRatio(480f/304f),contentScale=ContentScale.Fit)
                            if(selected) Surface(Modifier.align(Alignment.BottomEnd).padding(4.dp),
                                shape=RoundedCornerShape(12.dp),color=MaterialTheme.colorScheme.primary) {
                                Icon(Icons.Outlined.CheckCircle,"Diseño seleccionado",Modifier.padding(3.dp).size(18.dp),
                                    tint=MaterialTheme.colorScheme.onPrimary)
                            }
                        }
                        Text(design.name,style=MaterialTheme.typography.labelLarge,minLines=2,
                            modifier=Modifier.padding(top=8.dp))
                        Text(design.region,style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                        if(design.edition.isNotBlank()) Text(design.edition,style=MaterialTheme.typography.labelSmall,
                            color=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.padding(top=3.dp))
                    }
                }
            }
            if(row.size==1) Spacer(Modifier.weight(1f))
        }
    }
    Text("Imágenes de referencia de American Express. Orel es una aplicación independiente. El diseño solo se aplica en Orel.",
        style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant,
        modifier=Modifier.padding(top=8.dp,bottom=16.dp))
}
