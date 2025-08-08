class GoogleMapsConfig {
  // Coordenadas por defecto (Aguascalientes, México)
  static const double defaultLatitude = 21.8853;
  static const double defaultLongitude = -102.2916;
  static const double defaultZoom = 14.0;
  
  // API Key (debe configurarse en producción)
  static const String apiKey = 'YOUR_GOOGLE_MAPS_API_KEY';
  
  // Estilo del mapa (opcional)
  static const String mapStyle = '''
  [
    {
      "elementType": "geometry",
      "stylers": [
        {
          "color": "#f5f5f5"
        }
      ]
    },
    {
      "elementType": "labels.icon",
      "stylers": [
        {
          "visibility": "off"
        }
      ]
    },
    {
      "elementType": "labels.text.fill",
      "stylers": [
        {
          "color": "#616161"
        }
      ]
    },
    {
      "elementType": "labels.text.stroke",
      "stylers": [
        {
          "color": "#f5f5f5"
        }
      ]
    },
    {
      "featureType": "poi.park",
      "elementType": "geometry",
      "stylers": [
        {
          "color": "#e5e5e5"
        }
      ]
    }
  ]
  ''';
}