// TODO: onload function should retrieve the data needed to populate the UI
 function openNav() {
  document.getElementById("mySidebar").style.width = "200px";
  document.getElementById("main").style.marginLeft = "200px";
}

function closeNav() {
  document.getElementById("mySidebar").style.width = "0";
  document.getElementById("main").style.marginLeft= "0";
}

// Function to handle errors in fetch requests
function handleFetchError(error) {
  console.error('Error fetching data:', error);
}
// Function to add a row to the table
function addRow(testFile) {
  let table = document.getElementById('chart').getElementsByTagName('tbody')[0];
  let newRow = table.insertRow();

  let filenameCell = newRow.insertCell(0);
  let spamProbabilityCell = newRow.insertCell(1);
  let classCell = newRow.insertCell(2);

  filenameCell.textContent = testFile.filename;
  spamProbabilityCell.textContent = testFile.spamProbability;
  classCell.textContent = testFile.actualClass;
}

// Function to update accuracy value in the UI
function updateAccuracy() {
  fetch('http://localhost:8080/spamDetector-1.0/api/spam/accuracy', {
    method: 'GET',
    headers: {
      'Accept': 'application/json'
    }
  })
    .then(response => response.json())
    .then(data => {
      document.getElementById('accuracy').setAttribute('value', data.toString());
    })
    .catch(handleFetchError);
}

// Function to update precision value in the UI
function updatePrecision() {
  fetch('http://localhost:8080/spamDetector-1.0/api/spam/precision', {
    method: 'GET',
    headers: {
      'Accept': 'application/json'
    }
  })
    .then(response => response.json())
    .then(data => {
      document.getElementById('precision').setAttribute('value', data.toString());
    })
    .catch(handleFetchError);
}

// Call functions to update accuracy and precision values
document.addEventListener('DOMContentLoaded', function() {
  updateAccuracy();
  updatePrecision();
});

// Fetch data from the backend and add rows to the table
fetch('http://localhost:8080/spamDetector-1.0/api/spam')
  .then(response => response.json())
  .then(data => {
    data.forEach(testFile => {
      addRow(testFile);
    });
  })
  .catch(handleFetchError);
