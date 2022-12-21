/**
 * Sample React Native App
 * https://github.com/facebook/react-native
 *
 * @format
 * @flow strict-local
 */

import React, {useEffect, useState} from 'react';
 import { NativeModules } from 'react-native';
  const { SpotifyRemoteModule } = NativeModules;

import {
  SafeAreaView,
  ScrollView,
  StatusBar,
  StyleSheet,
  Text,
  useColorScheme,
  View,
  Platform, TouchableOpacity, Image
} from 'react-native';

import {
  Colors,
  DebugInstructions,
  Header,
  LearnMoreLinks,
  ReloadInstructions,
} from 'react-native/Libraries/NewAppScreen';

const App = () => {
 const [trackName, setTrackName] = useState('');
 
 handleName = (text) => {
  this.setState({ trackName: text })
}

  const connectToRemoteApp = () => {
    Platform.OS === 'android' && SpotifyRemoteModule.connect(true)
  };
  const playpause = () => {
    Platform.OS === 'android' && SpotifyRemoteModule.onPlayPauseButtonClicked()
  };
  const onSubmit = async () => {
    try {
      const name = await SpotifyRemoteModule.onPlayPauseButtonClicked(
      );
      console.log(`Created a new event with id ${name}`);
      setTrackName(name)
    } catch (e) {
      console.error(e);
    }
    
  };

  return (
    <View style={styles.container}>
      <StatusBar barStyle="dark-content" backgroundColor={'#e4e5ea'} />
      <Text style={styles.title}>Spotify Demo</Text>
      <Text style={styles.title}> Spotify remote player </Text>
      <View style={styles.iconsContainer}>
     
        <TouchableOpacity onPress={() => connectToRemoteApp()}>
          <Image
          source={require('./images/connect.png')} 
            // source={{
            //   uri:  
            //     // 'https://icons.iconarchive.com/icons/google/noto-emoji-smileys/256/10101-alien-icon.png',
            // }}
            resizeMode={'contain'}
            style={styles.icon}
          />
          <Text>connect</Text>
        </TouchableOpacity>
        <TouchableOpacity onPress={() =>onSubmit()}>
          <Image
          source={require('./images/play_pause_button.png')} 
            
            resizeMode={'contain'}
            style={styles.icon}
          />
          <Text>Play/Pause</Text>
        </TouchableOpacity>
        <Text>{trackName}</Text>
     
       
      </View>
    </View>
  );
};
// Styles are unchanged
const styles = StyleSheet.create({
  container: {
    backgroundColor: '#e4e5ea',
    flex: 1,
    paddingTop: 50,
    alignItems: 'center',
  },
  title: {
    fontSize: 20,
    color: '#000',
    marginVertical: 25,
  },
  iconsContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-evenly',
    width: '100%',
    paddingHorizontal: 50,
  },
  warningText: {
    color: 'red',
    fontWeight: 'bold',
    letterSpacing: 1.5,
    textAlign: 'center',
  },
  spacing: {
    marginVertical: 10,
  },
  row: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    width: '40%',
  },
  icon: {
    height: 40,
    width: 40,
    marginBottom: 15,
  },
});

export default App;