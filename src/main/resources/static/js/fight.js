import { models } from "./common.js";

document.addEventListener("DOMContentLoaded", function () {
  const textarea = document.getElementById('fight-input');
  const button = document.getElementById('fight-send-button');
  const chatContent = document.getElementById('chat-content');
  const heightWithUnit = textarea.style.height;
  const heightValue = parseFloat(heightWithUnit);


  const dropdownContainers = document.querySelectorAll('.dropdown-container');
  dropdownContainers.forEach(dropdownContainer => {
    const btnId = dropdownContainer.getAttribute('btn-id');
    const button = document.getElementById(btnId);
    button.addEventListener('click', function () {
      //console.log(dropdownContainer.classList.contains('show'));
      if (!dropdownContainer.classList.contains('show')) {
        //console.log(dropdownContainer);
        dropdownContainer.classList.toggle('show');
        dropdownContainer.style.width = button.offsetWidth + 'px'; // 设置dropdown宽度与按钮一致
      } else {
        dropdownContainer.classList.remove("show");
      }
    });
    models.forEach(model => {
      const modelDiv = document.createElement("div");
      modelDiv.textContent = model;
      modelDiv.addEventListener("click", function () {
        const modelId = dropdownContainer.getAttribute('mx-id');
        const fightmodel = document.getElementById(modelId);
        fightmodel.textContent = model;
        dropdownContainer.classList.remove("show");

      });
      dropdownContainer.appendChild(modelDiv);
    });
  });


  // document.addEventListener('click', function (event) {
  //   if (!dropdownContainer.contains(event.target)) {
  //     dropdownContainer.style.display = 'none';
  //   }
  // });



  //alert(heightValue)
  // 初始化文本框高度
  // const initialHeight = 40 + 'px';
  textarea.addEventListener('input', () => {

    // 获取包含单位的高度值，例如 "100.5px"
    // 提取数值部分，结果是 100.5

    textarea.style.height = 'auto';  // 重置高度
    textarea.style.height = textarea.scrollHeight + 'px';  // 设置新的高度
    const b = parseFloat(textarea.style.height);

    // 获取窗口高度的80% (a)
    const a = window.innerHeight * 0.8;
    // 调整chatContent的高度，转换为vh单位
    const newHeightInPx = a - b + heightValue;
    let newHeightInVh = (newHeightInPx / window.innerHeight) * 100;
    //alert(newHeightInVh)
    // 确保newHeightInVh的最大值为54
    if (newHeightInVh <= 56 || newHeightInVh <= 0) {
      newHeightInVh = 56;
    }

    // 设置chatContent的高度为vh单位
    chatContent.style.height = `${newHeightInVh}vh`;
    if (textarea.value.trim().length > 0) {
      button.disabled = false;
      button.classList.remove('disabled-btn');
    } else {
      //textarea.style.height = initialHeight + 'px';
      chatContent.style.height = 80 + 'vh';
      button.disabled = true;
      button.classList.add('disabled-btn');
    }
  });

  button.addEventListener('keydown', function () {

  })


});
