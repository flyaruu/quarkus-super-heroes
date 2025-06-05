import http from 'k6/http';
import { check } from 'k6';
import { randomFight } from './k6/randomFight.js';

export const options = {
  scenarios: {
    ramp_up: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '10s', target: 100 },
        { duration: '1m', target: 100 },
      ],
      gracefulRampDown: '0s',
    },
  },
};


export default () => {
  var response = http.get("http://localhost:8082/api/fights/randomfighters");
  check(response, {
      'random fighters status is 200': (r) => r.status === 200,
  });
  var response_body = JSON.parse(response.body);
  var hero = response_body.hero;
  check(location, {
    'hero is not fallback': (r) => !hero.name.toLowerCase().includes("fallback"),
  })
}




// export default () => {
//   var fight_result = randomFight();
//   console.log(fight_result.winnerName);
// }
