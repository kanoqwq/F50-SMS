//色相
let currentHue = 0;
//透明度
let currentOpacity = 1;
// 亮度
let currentValue = 1;
//饱和度
let currentSaturation = 1;
//字体颜色
let currentTextColor = 0;

// 调色盘
function getColorByPercent(e) {
    const value = e.target.value; // 0 ~ 100
    const h = (value / 100) * 300;
    currentHue = h;
    updateColor();
    //保存进度到localStorage
    localStorage.setItem('colorPer', value);
}

//亮度
function getValueByPercent(e) {
    const value = e.target.value;
    currentValue = value / 100;
    updateColor();
    localStorage.setItem('brightPer', value);
}

// 透明度
function getOpacityByPercent(e) {
    const value = e.target.value; // 0 ~ 100
    currentOpacity = value / 100;
    updateColor();
    //保存进度到localStorage
    localStorage.setItem('opacityPer', value);
}

//饱和度
function getSaturationByPercent(e) {
    const value = e.target.value; // 0 ~ 100
    currentSaturation = value / 100;
    updateColor();
    //保存进度到localStorage
    localStorage.setItem('saturationPer', value);
}

//字体颜色
function updateTextColor(e) {
    const value = e.target.value; // 0 ~ 100
    const gray = Math.round((value / 100) * 255);
    const color = `rgb(${gray}, ${gray}, ${gray})`;
    currentTextColor = color;
    updateColor()
    //保存进度到localStorage
    localStorage.setItem('textColorPer', value);
    localStorage.setItem('textColor', color);
}

// 更新颜色 + 透明度
function updateColor() {
    const { r, g, b } = hsvToRgb(currentHue, currentSaturation, currentValue);
    const color = `rgba(${r}, ${g}, ${b}, ${currentOpacity})`;
    // 修改 :root 中的 CSS 变量
    document.documentElement.style.setProperty('--dark-bgi-color', color);
    document.documentElement.style.setProperty('--dark-tag-color', color);
    document.documentElement.style.setProperty('--dark-text-color', currentTextColor);
    //保存到localStorage
    localStorage.setItem('themeColor', currentHue);
}

//读取颜色数据
const initTheme = () => {
    let color = localStorage.getItem('themeColor');
    let colorPer = localStorage.getItem('colorPer');
    let opacityPer = localStorage.getItem('opacityPer');
    let value = localStorage.getItem('brightPer');
    let saturation = localStorage.getItem('saturationPer');
    let textColor = localStorage.getItem('textColor');
    let textColorPer = localStorage.getItem('textColorPer');

    if (color == null || color == undefined) {
        color = 183;
        localStorage.setItem('themeColor', color);
    }
    if (colorPer == null || colorPer == undefined) {
        colorPer = 61;
        localStorage.setItem('colorPer', colorPer);
    }
    if (opacityPer == null || opacityPer == undefined) {
        opacityPer = 37;
        localStorage.setItem('opacityPer', opacityPer);
    }
    if (value == null || value == undefined) {
        value = 16;
        localStorage.setItem('brightPer', value);
    }
    if (saturation == null || saturation == undefined) {
        saturation = 16;
        localStorage.setItem('saturationPer', saturation);
    }
    if (textColor == null || textColor == undefined) {
        textColor = 'rgba(255, 255, 255, 1)';
        localStorage.setItem('textColor', textColor);
    }
    if (textColorPer == null || textColorPer == undefined) {
        textColorPer = 100;
        localStorage.setItem('textColorPer', textColorPer);
    }
    
    currentHue = color;
    currentOpacity = opacityPer / 100;
    currentValue = value / 100;
    currentSaturation = saturation / 100;
    currentTextColor = textColor;
    updateColor()
    document.querySelector("#colorEl").value = colorPer;
    document.querySelector("#opacityEl").value = opacityPer;
    document.querySelector("#brightEl").value = value;
    document.querySelector("#saturationEl").value = saturation;
    document.querySelector("#textColorEl").value = textColorPer;
}
initTheme()