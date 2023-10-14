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
      <v-col class="pa-5">
        <div class="d-flex justify-space-between mb-1" style="height: 28px !important;">
          <div>Before</div>
          <div>
          <v-row no-gutters class="ma-0 pa-0">
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
            <v-btn small outlined @click="$refs.uploader.click()" class="mr-2">Upload</v-btn>
            <v-btn small outlined @click="curImage=''">Clear</v-btn>
          </v-row> 
          </div>         
        </div>
        <div v-if="curImage==''" style="border: 1px solid green; height: 450px"></div>
        <vue-painting
        v-else
        style="height: 600px;"
        :img="curImage"
        @saveImage="onSaveImage"
        ></vue-painting>
      </v-col>
      <v-divider vertical></v-divider>
      <v-col class="pa-5">
        <v-row no-gutters class="mb-1 d-flex justify-space-between">
          <div>After</div>
          <v-btn small outlined @click="downPaint()" v-if="paintImage!=''">Download</v-btn>
          </v-row>
          <div v-if="paintImage==''" style="border: 1px solid green; height: 450px"></div>
          <v-img v-else
          :src="paintImage">
          </v-img>
      </v-col>
    </v-row>
    
    
  </div>
</template>

<script>
import VuePainting from "vue-painting"
export default {
  name: 'HelloWorld',
  components:{
    VuePainting,
  },
  props: {
    msg: String
  },
  data(){
    return {
      curImage: "",
      paintImage: "",
      urlImage: "",
    }
  },
  methods:{
    uploadFile(){
      this.$refs.uploader.click()
    },
    onSaveImage (blobFile) {
      console.log(blobFile)

      // upload to server to paint
      const reader = new FileReader();
        reader.onloadend = () => {
            const base64String = reader.result
            console.log("call api here")
            this.paintImage = base64String;
        };
        reader.readAsDataURL(blobFile);
    },
    onFileChange(e){
      this.curImage = "";
      const file = e.target.files[0];
      console.log(file)
        // Encode the file using the FileReader API
        const reader = new FileReader();
        reader.onloadend = () => {
            // Use a regex to remove data url part
            const base64String = reader.result
            this.curImage = base64String;

        };
        reader.readAsDataURL(file);
    },
    downPaint(){
      console.log("download")
      var a = document.createElement('a')
      a.download = `painting-${+new Date}.png`
      a.href = this.paintImage
      a.click()
    },
    convertImageToBase64(imgUrl, callback) {
      const image = new Image();
      image.crossOrigin='anonymous';
      image.onload = () => {
        const canvas = document.createElement('canvas');
        const ctx = canvas.getContext('2d');
        canvas.height = image.naturalHeight;
        canvas.width = image.naturalWidth;
        ctx.drawImage(image, 0, 0);
        const dataUrl = canvas.toDataURL();
        callback && callback(dataUrl)
      }
      image.src = imgUrl;
    },

    convertImgToDataURLviaCanvas (url, callback) {
      var img = new Image();

      img.crossOrigin = 'Anonymous';

      img.onload = function() {
        var canvas = document.createElement('CANVAS');
        var ctx = canvas.getContext('2d');
        var dataURL;
        canvas.height = this.height;
        canvas.width = this.width;
        ctx.drawImage(this, 0, 0);
        dataURL = canvas.toDataURL();
        callback(dataURL);
        canvas = null;
      };

      img.src = url;
    },
    convertUrl2Base64(url){
      this.convertImgToDataURLviaCanvas( url, (base64_Str)=>{
        this.curImage = base64_Str;
      })
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
