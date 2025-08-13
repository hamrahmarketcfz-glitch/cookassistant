package com.example.cookassistant

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlin.random.Random

data class Dish(val name: String, val ingredients: List<String>, val steps: List<String>)
data class Person(val name: String, val allergies: List<String>, val likes: MutableSet<String> = mutableSetOf())

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AppUi() }
    }
}

@Composable
fun AppUi() {
    val nav = rememberNavController()
    val dishes = remember { seedDishes().toMutableStateList() }
    val persons = remember { mutableStateListOf<Person>() }
    val shopping = remember { mutableStateListOf<String>() }
    var lastSuggested by remember { mutableStateOf<Dish?>(null) }
    Scaffold(
        bottomBar = {
            NavigationBar {
                val items = listOf("suggest","dishes","family","shopping")
                val labels = mapOf("suggest" to "پیشنهاد", "dishes" to "غذاها", "family" to "خانواده", "shopping" to "خرید")
                val current = nav.currentBackStackEntry?.destination?.route ?: "suggest"
                items.forEach { r ->
                    NavigationBarItem(
                        selected = current == r,
                        onClick = { nav.navigate(r) { launchSingleTop = true } },
                        icon = { Icon(Icons.Filled.Favorite, contentDescription = labels[r]) },
                        label = { Text(labels[r] ?: r) }
                    )
                }
            }
        }
    ) { inner ->
        NavHost(navController = nav, startDestination = "suggest", modifier = Modifier.padding(inner)) {
            composable("dishes") {
                LazyColumn(Modifier.fillMaxSize().padding(16.dp)) {
                    items(dishes) { d ->
                        Card(Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                            Column(Modifier.padding(12.dp)) {
                                Text(d.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(6.dp))
                                Text("مواد لازم:"); d.ingredients.forEach { Text("- $it") }
                                Spacer(Modifier.height(6.dp))
                                Text("دستور پخت:"); d.steps.forEachIndexed { i, s -> Text("${i+1}. $s") }
                            }
                        }
                    }
                }
            }
            composable("family") {
                var name by remember { mutableStateOf("") }
                var allergy by remember { mutableStateOf("") }
                Column(Modifier.fillMaxSize().padding(16.dp)) {
                    Text("اعضای خانواده", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("نام") }, modifier = Modifier.weight(1f))
                        Spacer(Modifier.width(8.dp))
                        OutlinedTextField(value = allergy, onValueChange = { allergy = it }, label = { Text("حساسیت‌ها (با , جدا)") }, modifier = Modifier.weight(1f))
                        Spacer(Modifier.width(8.dp))
                        Button(onClick = {
                            if (name.isNotBlank()) {
                                val list = allergy.split(',').map { it.trim() }.filter { it.isNotEmpty() }
                                persons.add(Person(name.trim(), list))
                                name = ""; allergy = ""
                            }
                        }) { Text("افزودن") }
                    }
                    Spacer(Modifier.height(12.dp))
                    LazyColumn(Modifier.fillMaxSize()) {
                        items(persons) { p ->
                            Card(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                                Column(Modifier.padding(12.dp)) {
                                    Text(p.name, style = MaterialTheme.typography.titleMedium)
                                    Text("حساسیت‌ها: " + (if (p.allergies.isEmpty()) "—" else p.allergies.joinToString(", ")))
                                }
                            }
                        }
                    }
                }
            }
            composable("suggest") {
                var selected by remember { mutableStateOf<Person?>(null) }
                Column(Modifier.fillMaxSize().padding(16.dp)) {
                    Text("پیشنهاد غذای غیرتکراری", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        var expanded by remember { mutableStateOf(false) }
                        Box {
                            OutlinedButton(onClick = { expanded = true }) { Text(selected?.name ?: "انتخاب عضو خانواده (اختیاری)") }
                            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                DropdownMenuItem(text = { Text("بدون فرد مشخص") }, onClick = { selected = null; expanded = false })
                                persons.forEach { p -> DropdownMenuItem(text = { Text(p.name) }, onClick = { selected = p; expanded = false }) }
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                        Button(onClick = {
                            val allergies = selected?.allergies?.toSet() ?: emptySet()
                            val filtered = dishes.filter { d -> allergies.none { a -> d.ingredients.any { it.contains(a, true) } } }
                            val pick = if (filtered.isNotEmpty()) filtered.random() else dishes.random()
                            lastSuggested = pick
                        }) { Text("پیشنهاد بده") }
                        Spacer(Modifier.width(8.dp))
                        Button(onClick = {
                            val liked = selected?.likes ?: mutableSetOf()
                            if (liked.isNotEmpty()) {
                                val favs = dishes.filter { it.name in liked }
                                lastSuggested = favs.random()
                            }
                        }, enabled = selected != null) { Text("قرعه‌کشی علاقه‌مندی") }
                    }
                    Spacer(Modifier.height(12.dp))
                    val result = lastSuggested
                    if (result != null) {
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp)) {
                                Text(result.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(6.dp))
                                Text("مواد لازم:"); result.ingredients.forEach { Text("- $it") }
                                Spacer(Modifier.height(6.dp))
                                Row {
                                    Button(onClick = {
                                        shopping.clear()
                                        shopping.addAll(result.ingredients)
                                    }) { Text("افزودن به لیست خرید") }
                                    Spacer(Modifier.width(8.dp))
                                    if (selected != null) {
                                        val isFav = selected!!.likes.contains(result.name)
                                        Button(onClick = {
                                            if (isFav) selected!!.likes.remove(result.name) else selected!!.likes.add(result.name)
                                        }) { Text(if (isFav) "حذف از علاقه‌مندی" else "افزودن به علاقه‌مندی") }
                                    }
                                }
                            }
                        }
                    } else {
                        Text("برای شروع یک نفر را انتخاب کنید (اختیاری) و دکمه «پیشنهاد بده» را بزنید.")
                    }
                }
            }
            composable("shopping") {
                val ctx = LocalContext.current
                Column(Modifier.fillMaxSize().padding(16.dp)) {
                    Text("لیست خرید", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    LazyColumn(Modifier.weight(1f)) {
                        items(shopping) { s ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(s, modifier = Modifier.weight(1f))
                            }
                        }
                    }
                    val shareText = shopping.joinToString("\\n") { "- $it" }
                    Button(onClick = {
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, shareText)
                        }
                        ctx.startActivity(Intent.createChooser(intent, "اشتراک‌گذاری لیست خرید"))
                    }, enabled = shopping.isNotEmpty()) { Text("اشتراک‌گذاری") }
                }
            }
        }
    }
}

fun seedDishes(): List<Dish> = listOf(
    Dish("قورمه‌سبزی",
        listOf("گوشت خورشتی 400 گرم","سبزی قورمه 300 گرم","لوبیا قرمز 1 پیمانه","لیمو عمانی 2 عدد","پیاز 1 عدد"),
        listOf("پیاز را تفت دهید، گوشت را اضافه کنید.","سبزی را جدا تفت دهید و به قابلمه بیفزایید.","لوبیا خیس‌خورده و لیمو عمانی را اضافه و با آب بپزید.")
    ),
    Dish("خورش قیمه",
        listOf("گوشت خورشتی 300 گرم","لپه 1 پیمانه","سیب‌زمینی 2 عدد","رب گوجه 2 ق‌غ","پیاز 1 عدد"),
        listOf("پیاز و گوشت را تفت دهید.","لپه نیم‌پز و رب را اضافه کنید.","با لیمو عمانی جا بیفتد؛ سیب‌زمینی سرخ‌شده را آخر اضافه کنید.")
    ),
    Dish("زرشک‌پلو با مرغ",
        listOf("مرغ 4 تکه","زرشک 3 ق‌غ","برنج 2 پیمانه","پیاز 1 عدد","زعفران"),
        listOf("مرغ را با پیاز و ادویه سرخ و سپس با آب کم بپزید.","برنج را آبکش کنید.","زرشک را با کره و زعفران تفت دهید و با مرغ سرو کنید.")
    ),
    Dish("میرزا قاسمی",
        listOf("بادمجان 4 عدد","تخم‌مرغ 3 عدد","سیر 4 حبه","گوجه 2 عدد"),
        listOf("بادمجان‌ها را کبابی و له کنید.","سیر و گوجه را تفت دهید.","تخم‌مرغ اضافه و سریع هم بزنید.")
    )
)