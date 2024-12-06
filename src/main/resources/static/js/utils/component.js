function closeAlert() {
    const closeAlert = document.getElementById('closeAlert');
    closeAlert.addEventListener('click', () => {
        const alertBox = document.getElementById('customAlert');
        alertBox.classList.remove('open');
    })
    closeAlert.click();
}

let alertTimeout;
export function showAlert(message = "获取成功", isSuccess, duration = 3000) {
    const alertBox = document.getElementById('customAlert');
    const alertMessage = document.getElementById('alertMessage');
    alertMessage.textContent = message;
    alertBox.classList.add('open');
    if (isSuccess) {
        alertBox.classList.add('alert-success');
        alertBox.classList.remove('alert-failure');
    } else {
        alertBox.classList.add('alert-failure');
        alertBox.classList.remove('alert-success');
    }
    // 自动关闭弹窗
    if (alertTimeout) {
        clearTimeout(alertTimeout);
    }

    // 设置新的定时器
    alertTimeout = setTimeout(() => {
        closeAlert();
    }, duration);
}
