/** Browser GPS as a promise: { latitude, longitude } rounded to 7 decimals (DECIMAL(10,7)). */
export const getPosition = () => new Promise((resolve, reject) => {
  if (!navigator.geolocation) {
    reject(new Error('Location is not available in this browser'));
    return;
  }
  navigator.geolocation.getCurrentPosition(
    (p) => resolve({ latitude: round(p.coords.latitude), longitude: round(p.coords.longitude) }),
    (e) => reject(new Error(e.message || 'Could not read your location')),
    { enableHighAccuracy: true, timeout: 15000, maximumAge: 30000 },
  );
});

export const round = (n) => Math.round(n * 1e7) / 1e7;

export const mapsLink = (a) => (a
  ? `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(`${a.addressLine1}, ${a.city}, ${a.state} ${a.pincode}`)}`
  : null);
