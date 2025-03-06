import http from 'k6/http';

export const options = {
    vus: 10,
    duration: '40s'
};

export default() => {
    var response_body = JSON.parse(http.get("http://localhost:8082/api/fights/randomfighters").body);
    var hero = response_body.hero;
    var villain = response_body.villain;
    var location = JSON.parse(http.get("http://localhost:8082/api/fights/randomlocation").body);

    var fight_request = { hero: hero, villain: villain, location: location};
    var fight_result = JSON.parse(http.post("http://localhost:8082/api/fights", JSON.stringify(fight_request)).body);
    console.log(fight_result.winnerName);
}