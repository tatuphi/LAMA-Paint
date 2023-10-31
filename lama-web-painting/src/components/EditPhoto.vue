<template>
 <div id="app">
    <input
    ref="uploader"
    class="d-none"
    type="file"
    accept="*/image"
    @change="onFileChange"
    />
    <v-row no-gutters>
      <v-col class="pa-5" cols="12">
        <div class="d-flex justify-space-between mb-1" style="height: 28px !important;">
          <div>Upload a photo to edit</div>
          <div>
          <v-row no-gutters class="ma-0 pa-0">
            <div class="mr-2">
            <span>Size: </span>
            <input
              type="range"
              min="1"
              max="50"
              value="25"
              class="size"
              id="sizeRange"
            />
          </div>
            <v-text-field
              height="28"
              style="max-width: 250px !important;"
              class="mr-2"
              dense
              v-model="urlImage"
              placeholder="Photo Url"
              outlined
              @keydown.enter="convertUrl2Base64(urlImage)"
            ></v-text-field>
            <v-btn small outlined @click="uploadImage" class="mr-2" :disabled="isEditing">Upload</v-btn>
            <v-btn small outlined id="clear" class="mr-2" :disabled="isEditing">Clear</v-btn>
            <v-btn id="edit" :disabled="isEditing" dense color="primary" style="height: 28px" elevation="0">Edit</v-btn>
          </v-row> 
          </div>         
        </div>
        <div v-if="curImage==''" style="border: 1px solid green; height: 450px; "></div>
        <canvas
          v-else
          id="fcanvas"
          height="450"
          style="border: 1px solid green;"
          class="ma-auto"
        ></canvas>
      </v-col>
      <v-col class="pa-5" cols="12" id="edit-photo">
          <div v-if="isEditing">Let's get the result!</div>
          <div v-if="isEditing" style="border: 1px solid green; height: 450px;"
           class="d-flex justify-space-around">
            <v-img
            :src="require('../assets/loading.gif')"
            width="500px"
            height="450px"
            />
          </div>
          
          <v-row v-else-if="paintImage!=''">
            <v-col> 
            <div style="height:28px" class="mb-1">Before</div>
            <v-img
            :width="dfWidth"
            :src="curImage"
            style="border: 1px solid red"
            >
            </v-img>
            </v-col>
            <v-col>
            <div no-gutters class="mb-1 d-flex justify-space-between" style="height:28px">
            <div>After</div>
            <v-btn small outlined @click="downPaint()" v-if="paintImage!=''">Download</v-btn>
            </div>
            <v-img
            :width="dfWidth"
            :src="paintImage"
            style="border: 1px solid green">
            </v-img>
            </v-col>
          </v-row>
      </v-col>
    </v-row>
  </div>
</template>

<script>
import axios from "axios";
export default {
  name: 'HelloWorld',
  components:{
  },
  props: {
    msg: String
  },
  data(){
    return {
      isEditing: false,
      curImage: "",
      urlImage: "",
      paintImage: "",
      size: 10,
      color: "red",
      context: null,
      canvasElement: null,
      dfWidth:635,
      dfHeight: 450,
    }
  },
  mounted(){
    const sizeElement = document.querySelector("#sizeRange");
      this.size = sizeElement.value;
      sizeElement.oninput = (e) => {
        this.size = e.target.value;
      };
    // this.loadPOST()
  },
  methods:{
    loadPOST(){
      let dataObj = {
        judgement: "here i am"
      }
      axios.post("http://9145-35-193-98-61.ngrok.io/ner", dataObj)
      .then((response)=>{
        if(response.status==200){
          console.log(response.data)
        }
      })
      .catch(error => {
        // this.errorMessage = error.message;
        console.error("There was an error!", error);
      })
    },
    uploadImage(){
      this.$refs.uploader.click()
    },
    drawOnImage(image = null) {
      const canvasElement = document.getElementById("fcanvas");
      const context = canvasElement.getContext("2d");

      canvasElement.width = image.width;
      canvasElement.height = image.height;

      // if an image is present,
      // the image passed as a parameter is drawn in the canvas
      if (image) {
        // context.drawImage(image, 0, 0, this.dfWidth, this.dfHeight);
        context.drawImage(image, 0, 0, image.width, image.height);
      }

      const clearElement = document.getElementById("clear");
      clearElement.onclick = () => {
        context.clearRect(0, 0, image.width, image.height);
      };

      const editElement = document.getElementById("edit");
      editElement.onclick = () =>{
        this.isEditing = true;
        const element = document.getElementById('edit-photo');
        if (element) {
          element.scrollIntoView({ behavior: 'smooth', block: 'end' });
        }

        this.paintImage = ""
        const formData = new FormData();
        formData.append('image_raw', this.convertBase64ToFile(this.curImage, "image_raw.png"));
        formData.append('image_color',  this.convertBase64ToFile(canvasElement.toDataURL('image/png'), "image_color.png"));
        console.log(formData)
        let jjj = {
          'text': 'here i am'
        }
        axios.post("http://9145-35-193-98-61.ngrok.io/ner", jjj)
        .then((response)=>{
          if(response.status==200){
          //   this.paintImage = 
          // `data:image/${response.data.format.toLowerCase()};base64,` + response.data.img;
          console.log(response.data)
          }
          
        })
        .catch(error => {
          // this.errorMessage = error.message;
          console.error("There was an error!", error);
        })
        .finally(()=>{
          this.isEditing = false;
        })
      }

      let isDrawing;

      canvasElement.onmousedown = (e) => {
        isDrawing = true;
        context.beginPath();
        context.lineWidth = this.size;
        context.strokeStyle = this.color;
        context.lineJoin = "round";
        context.lineCap = "round";
        context.moveTo(e.clientX - 20, e.clientY - 116);
      };

      canvasElement.onmousemove = (e) => {
        if (isDrawing) {
          context.lineTo(e.clientX - 20, e.clientY - 116);
          context.stroke();
        }
      };

      canvasElement.onmouseup = function () {
        isDrawing = false;
        if(context){
          context.closePath();
        }
      };
    },
    onFileChange(e){
      // if(context){
      //   context.clearRect(0, 0, this.dfWidth, this.dfHeight);
      // }
      this.curImage = "";
      const file = e.target.files[0];
        const reader = new FileReader();
        reader.onloadend = () => {
            const base64String = reader.result
            this.curImage = base64String;
            
             const image = document.createElement("img");
             image.src = base64String;

              // enbaling the brush after after the image
              // has been uploaded
              image.addEventListener("load", () => {
                this.drawOnImage(image);
              });
        };
        reader.readAsDataURL(file);
    },

    downPaint(){
      var a = document.createElement('a')
      a.download = `painting-${+new Date}.png`
      a.href = this.paintImage
      a.click()
    },

    convertImgToDataURLviaCanvas (url, callback) {
      var img = new Image();

      img.crossOrigin = 'Anonymous';

      img.onload = function() {
        var canvas = document.createElement('CANVAS');
        var ctx = canvas.getContext('2d');
        var dataURL;
        canvas.height = this.height;
        // image.naturalHeight
        canvas.width = this.width;
        // image.naturalWidth

          canvas.width = img.width;
          canvas.height = img.height;

          ctx.clearRect(0, 0, canvas.width, canvas.height);
          ctx.drawImage(img, 0, 0, img.width, img.height);

        // ctx.drawImage(this, 0, 0);
        dataURL = canvas.toDataURL();
        callback(dataURL);
        canvas = null;
      };

      img.src = url;
    },
    convertUrl2Base64(url){
      console.log("here iam")
      this.curImage = "";
      this.convertImgToDataURLviaCanvas( url, (base64_Str)=>{
        this.curImage = base64_Str;
        const image = document.createElement("img");
        image.src = base64_Str;

        // enbaling the brush after after the image
        // has been uploaded
        image.addEventListener("load", () => {
          this.drawOnImage(image);
        });
      })
    },
    convertBase64ToFile(base64Data, fileName) {
      console.log(base64Data)
      const byteCharacters = window.atob(base64Data.split(",")[1]);
      const byteNumbers = new Array(byteCharacters.length);

      for (let i = 0; i < byteCharacters.length; i++) {
        byteNumbers[i] = byteCharacters.charCodeAt(i);
      }

      const byteArray = new Uint8Array(byteNumbers);
      const file = new Blob([byteArray], { type: 'application/octet-stream' });

      return new File([file], fileName);
    },
  },
}
</script>

<style >
.v-text-field .v-input__control .v-input__slot {
    min-height: auto !important;
    display: flex !important;
    align-items: center !important;
  }
</style>
