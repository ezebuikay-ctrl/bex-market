package com.bexmarket.ng.app

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThreeDPriceSlider(
    value: ClosedFloatingPointRange<Float>,
    onValueChange: (ClosedFloatingPointRange<Float>) -> Unit,
    valueRange: ClosedFloatingPointRange<Float> = 0f..100000f,
    onValueChangeFinished: (() -> Unit)? = null
) {
    val primaryColor = Color(0xFF006400) // Dark Green
    
    val startInteractionSource = remember { MutableInteractionSource() }
    val endInteractionSource = remember { MutableInteractionSource() }
    
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "Min: ₦${value.start.toInt()}", 
                fontSize = 12.sp, 
                fontWeight = FontWeight.Bold,
                color = primaryColor
            )
            Text(
                "Max: ₦${value.endInclusive.toInt()}${if(value.endInclusive >= valueRange.endInclusive) "+" else ""}", 
                fontSize = 12.sp, 
                fontWeight = FontWeight.Bold,
                color = primaryColor
            )
        }
        
        RangeSlider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            onValueChangeFinished = onValueChangeFinished,
            startInteractionSource = startInteractionSource,
            endInteractionSource = endInteractionSource,
            startThumb = {
                ThreeDThumb(label = "₦${value.start.toInt()}", interactionSource = startInteractionSource)
            },
            endThumb = {
                ThreeDThumb(label = "₦${value.endInclusive.toInt()}", interactionSource = endInteractionSource)
            },
            track = { rangeSliderState ->
                SliderDefaults.Track(
                    rangeSliderState = rangeSliderState,
                    modifier = Modifier
                        .height(10.dp)
                        .shadow(elevation = 2.dp, shape = RoundedCornerShape(5.dp), clip = false)
                        .border(1.dp, Color.LightGray.copy(alpha = 0.5f), RoundedCornerShape(5.dp)),
                    colors = SliderDefaults.colors(
                        activeTrackColor = primaryColor,
                        inactiveTrackColor = Color(0xFFE0E0E0)
                    )
                )
            }
        )
    }
}

@Composable
fun ThreeDThumb(label: String, interactionSource: MutableInteractionSource) {
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 1.2f else 1.0f, label = "thumbScale")
    val elevation by animateFloatAsState(if (isPressed) 8.dp.value else 4.dp.value, label = "thumbElevation")

    Box(contentAlignment = Alignment.Center) {
        // Floating Bubble Label
        if (isPressed) {
            Surface(
                modifier = Modifier
                    .offset(y = (-45).dp)
                    .shadow(6.dp, RoundedCornerShape(8.dp)),
                color = Color(0xFF006400),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = label,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // 3D Style Thumb
        Box(
            modifier = Modifier
                .size(24.dp)
                .shadow(elevation.dp, CircleShape)
                .clip(CircleShape)
                .background(Color.White)
                .border(2.dp, Color(0xFF006400), CircleShape)
                .padding(2.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(Color(0xFFE8F5E9))
            )
        }
    }
}
