package mil.nga.giat.mage.form.defaults

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.ButtonDefaults.textButtonColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LiveData
import mil.nga.giat.mage.form.FormState
import mil.nga.giat.mage.form.edit.FieldEditContent
import mil.nga.giat.mage.form.field.FieldState
import mil.nga.giat.mage.ui.theme.*

@Composable
fun FormDefaultScreen(
  formStateLiveData: LiveData<FormState?>,
  onClose: (() -> Unit)? = null,
  onSave: (() -> Unit)? = null,
  onReset: (() -> Unit)? = null,
  onFieldClick: ((FieldState<*, *>) -> Unit)? = null
) {
  val formState by formStateLiveData.observeAsState()
  val scope = rememberCoroutineScope()
  val scaffoldState = rememberScaffoldState()

  MageTheme {
    Scaffold(
      scaffoldState = scaffoldState,
      topBar = {
        TopBar(
          formName = formState?.definition?.name,
          onClose = { onClose?.invoke() },
          onSave = {
            onSave?.invoke()
          }
        )
      },
      content = {
        Content(
          formState = formState,
          onReset = {
            onReset?.invoke()
          },
          onFieldClick = onFieldClick
        )
      }
    )
  }
}

@Composable
fun TopBar(
  formName: String?,
  onClose: () -> Unit,
  onSave: () -> Unit
) {
  TopAppBar(
    backgroundColor = MaterialTheme.colors.topAppBarBackground,
    contentColor = Color.White,
    title = {
      Column {
        Text(formName ?: "")
      }
    },
    navigationIcon = {
      IconButton(onClick = { onClose.invoke() }) {
        Icon(Icons.Default.Close, "Cancel Default")
      }
    },
    actions = {
      TextButton(
        colors = textButtonColors(contentColor = Color.White),
        onClick = { onSave() }
      ) {
        Text("SAVE")
      }
    }
  )
}

@Composable
fun Content(
  formState: FormState?,
  onReset: (() -> Unit)? = null,
  onFieldClick: ((FieldState<*, *>) -> Unit)? = null
) {
  Surface() {
    Column(
      Modifier
        .fillMaxHeight()
        .background(Color(0x19000000))
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 8.dp)
    ) {
      if (formState?.definition != null) {
        DefaultHeader(
          name = formState.definition.name,
          description = formState.definition.description,
          color = formState.definition.hexColor
        )

        DefaultContent(
          formState = formState,
          onReset = onReset,
          onFieldClick = onFieldClick
        )
      }
    }
  }
}

@Composable
fun DefaultHeader(
  name: String,
  description: String?,
  color: String
) {
  Row(
    modifier = Modifier.padding(vertical = 16.dp)
  ) {
    Icon(
      imageVector = Icons.Default.Description,
      contentDescription = "Form",
      tint = Color(android.graphics.Color.parseColor(color)),
      modifier = Modifier
        .width(40.dp)
        .height(40.dp)
    )

    Column(
      Modifier
        .weight(1f)
        .padding(start = 16.dp)
        .fillMaxWidth()
    ) {
      Text(
        text = name,
        style = MaterialTheme.typography.h6
      )

      CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
        if (description?.isNotEmpty() == true) {
          Text(
            text = description,
            style = MaterialTheme.typography.body2
          )
        }
      }
    }
  }
}

@Composable
fun DefaultContent(
  formState: FormState,
  onReset: (() -> Unit)? = null,
  onFieldClick: ((FieldState<*, *>) -> Unit)? = null
) {
  Card(Modifier.padding(bottom = 16.dp)) {
    Column(Modifier.padding(16.dp)) {
      Text(
        text = "Custom Form Defaults",
        style = MaterialTheme.typography.h6,
        modifier = Modifier.padding(bottom = 8.dp)
      )

      CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
        Text(
          text = "Personalize the default values MAGE will autofill when you add this form to an observation.",
          style = MaterialTheme.typography.body2,
          modifier = Modifier.padding(bottom = 16.dp)
        )
      }

      DefaultFormContent(formState = formState, onFieldClick = onFieldClick)

      Divider(Modifier.padding(vertical = 16.dp))

      Row(
        horizontalArrangement = Arrangement.End,
        modifier = Modifier.fillMaxWidth()
      ) {
        TextButton(
          onClick = { onReset?.invoke() },
          colors = textButtonColors(contentColor = MaterialTheme.colors.error)
        ) {
          Text(text = "RESET TO SERVER DEFAULTS")
        }
      }
    }
  }
}

@Composable
fun DefaultFormContent(
  formState: FormState,
  onFieldClick: ((FieldState<*, *>) -> Unit)? = null
) {
  for (fieldState in formState.fields.sortedBy { it.definition.id }) {
    FieldEditContent(
      fieldState,
      onClick = {
        onFieldClick?.invoke(fieldState)
      }
    )
  }
}
